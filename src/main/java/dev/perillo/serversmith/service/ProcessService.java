package dev.perillo.serversmith.service;

import dev.perillo.serversmith.model.ServerInstance;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ProcessService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessService.class);
    private final Map<String, ServerProcess> processes = new ConcurrentHashMap<>();

    public void startServer(ServerInstance instance, int minMem, int maxMem, Consumer<String> onLog)
            throws IOException {
        if (processes.containsKey(instance.getLocation().toString())) {
            throw new IllegalStateException("Server already running");
        }

        File instanceDir = instance.getLocation().toFile();
        // Find jar? Usually "server.jar" but NeoForge might be different?
        // Let's assume server.jar or find standard names.
        // Prompt says: "Create instance... server.jar or relevant runtime files".
        // For NeoForge it might be `run.sh` or `run.bat` or a specific jar.
        // However, prompt says: Start: spawn a ProcessBuilder("java", "-Xmx<mem>", ...,
        // "-jar", "<jar>", "nogui")
        // So we look for the main jar.

        File jarFile = new File(instanceDir, "server.jar");
        if (!jarFile.exists()) {
            // Try to find any jar? Or specific neoforged?
            // NeoForge installers usually create a run script. But newer ones might just
            // rely on java -jar keys.
            // Let's trust "server.jar" convention for now.
            // The prompt says "NeoForge: ... place output in instance folder." Installer
            // might create `server.jar` or user renames it?
            // Usually NeoForge 1.20+ uses `run.bat` / `run.sh` which calls
            // `user_jvm_args.txt`.
            // But prompt explicitly asked for `ProcessBuilder("java" ... "-jar")`.
            // We'll stick to finding a jar. If multiple, pick the biggest one or one
            // containing "forge" / "server"?
            // Simplest: Check for `server.jar`. If not, check `neoforge-*.jar` or
            // `forge-*.jar`.
            File[] jars = instanceDir.listFiles((d, name) -> name.endsWith(".jar")
                    && (name.contains("server") || name.contains("forge") || name.contains("neoforge")));
            if (jars != null && jars.length > 0) {
                jarFile = jars[0]; // naive pick
            } else {
                throw new IOException("No server jar found in " + instanceDir);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Xms" + minMem + "M",
                "-Xmx" + maxMem + "M",
                "-jar",
                jarFile.getName(),
                "nogui");
        pb.directory(instanceDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        ServerProcess serverProcess = new ServerProcess(process, instance, onLog);
        processes.put(instance.getLocation().toString(), serverProcess);

        // Update status
        instance.setRunning(true); // Should notify UI if bound

        // Start log reader
        new Thread(serverProcess::readLog).start();

        // On exit
        process.onExit().thenRun(() -> {
            processes.remove(instance.getLocation().toString());
            Platform.runLater(() -> instance.setRunning(false));
            if (onLog != null)
                onLog.accept("Server stopped.");
        });
    }

    public void stopServer(ServerInstance instance) {
        ServerProcess proc = processes.get(instance.getLocation().toString());
        if (proc != null) {
            proc.sendCommand("stop");
        }
    }

    public void killServer(ServerInstance instance) {
        ServerProcess proc = processes.get(instance.getLocation().toString());
        if (proc != null) {
            proc.process.destroyForcibly();
        }
    }

    public void sendCommand(ServerInstance instance, String command) {
        ServerProcess proc = processes.get(instance.getLocation().toString());
        if (proc != null) {
            proc.sendCommand(command);
        }
    }

    public boolean isRunning(ServerInstance instance) {
        return processes.containsKey(instance.getLocation().toString());
    }

    private static class ServerProcess {
        final Process process;
        final ServerInstance instance;
        final Consumer<String> onLog;

        ServerProcess(Process process, ServerInstance instance, Consumer<String> onLog) {
            this.process = process;
            this.instance = instance;
            this.onLog = onLog;
        }

        void readLog() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (onLog != null) {
                        String finalLine = line;
                        Platform.runLater(() -> onLog.accept(finalLine));
                    }
                }
            } catch (IOException e) {
                logger.error("Error reading log for " + instance.getName(), e);
            }
        }

        void sendCommand(String cmd) {
            try {
                OutputStream out = process.getOutputStream();
                out.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException e) {
                logger.error("Error sending command to " + instance.getName(), e);
            }
        }
    }
}
