package dev.perillo.serversmith.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.perillo.serversmith.util.FileUtil;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DownloadService {
    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public CompletableFuture<List<String>> fetchVanillaVersions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = mapper.readTree(response.body());
                List<String> versions = new ArrayList<>();
                if (root.has("versions")) {
                    for (JsonNode node : root.get("versions")) {
                        if (node.get("type").asText().equals("release")) {
                            versions.add(node.get("id").asText());
                        }
                    }
                }
                return versions;
            } catch (Exception e) {
                logger.error("Failed to fetch vanilla versions", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<List<String>> fetchPaperVersions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.papermc.io/v2/projects/paper"))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = mapper.readTree(response.body());
                List<String> versions = new ArrayList<>();
                if (root.has("versions")) {
                    for (JsonNode node : root.get("versions")) {
                        versions.add(node.asText());
                    }
                }
                // Reverse to get latest first usually? Paper API lists oldest to newest.
                return versions.reversed();
            } catch (Exception e) {
                logger.error("Failed to fetch paper versions", e);
                throw new RuntimeException(e);
            }
        });
    }

    // NeoForge versions are tricky, depends on how deep we want to go.
    // For now, let's just return a static list or try to fetch from maven XML if
    // possible?
    // Let's stick to a simple list for NeoForge or skip version fetching for now if
    // it's too complex without parsing XML.
    // Actually, simple way:
    // https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge
    public CompletableFuture<List<String>> fetchNeoForgeVersions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge"))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = mapper.readTree(response.body());
                List<String> versions = new ArrayList<>();
                if (root.has("versions")) {
                    for (JsonNode node : root.get("versions")) {
                        versions.add(node.asText());
                    }
                }
                return versions.reversed();
            } catch (Exception e) {
                logger.error("Failed to fetch neoforge versions", e);
                // Fallback
                return List.of("20.4.80-beta", "20.2.86");
            }
        });
    }

    public CompletableFuture<Void> downloadFile(String url, Path destination, Consumer<Double> progress) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Check cache first? Prompt says "Cache downloaded jars".
                String fileName = url.substring(url.lastIndexOf('/') + 1);
                Path cachedFile = FileUtil.CACHE_DIR.resolve(fileName);

                if (Files.exists(cachedFile)) {
                    // Check size or something? For now assume valid if exists.
                    logger.info("Using cached file: " + cachedFile);
                    Files.createDirectories(destination.getParent());
                    Files.copy(cachedFile, destination, StandardCopyOption.REPLACE_EXISTING);
                    if (progress != null)
                        Platform.runLater(() -> progress.accept(1.0));
                    return;
                }

                Files.createDirectories(FileUtil.CACHE_DIR);
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<java.io.InputStream> response = client.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                long totalBytes = response.headers().firstValueAsLong("content-length").orElse(-1L);
                long readBytes = 0;

                try (BufferedInputStream in = new BufferedInputStream(response.body());
                        FileOutputStream out = new FileOutputStream(cachedFile.toFile())) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        readBytes += bytesRead;
                        if (progress != null && totalBytes > 0) {
                            double p = (double) readBytes / totalBytes;
                            Platform.runLater(() -> progress.accept(p));
                        }
                    }
                }

                // Copy to destination
                Files.createDirectories(destination.getParent());
                Files.copy(cachedFile, destination, StandardCopyOption.REPLACE_EXISTING);

            } catch (Exception e) {
                logger.error("Download failed: " + url, e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> getVanillaDownloadUrl(String version) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = mapper.readTree(response.body());
                String versionUrl = null;
                if (root.has("versions")) {
                    for (JsonNode node : root.get("versions")) {
                        if (node.get("id").asText().equals(version)) {
                            versionUrl = node.get("url").asText();
                            break;
                        }
                    }
                }

                if (versionUrl == null)
                    throw new IllegalArgumentException("Version not found: " + version);

                request = HttpRequest.newBuilder().uri(URI.create(versionUrl)).GET().build();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode versionMeta = mapper.readTree(response.body());
                return versionMeta.get("downloads").get("server").get("url").asText();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> getPaperDownloadUrl(String version) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Get builds
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.papermc.io/v2/projects/paper/versions/" + version + "/builds"))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = mapper.readTree(response.body());

                JsonNode builds = root.get("builds");
                JsonNode latestBuild = builds.get(builds.size() - 1);
                int buildNum = latestBuild.get("build").asInt();
                String fileName = latestBuild.get("downloads").get("application").get("name").asText();

                return "https://api.papermc.io/v2/projects/paper/versions/" + version + "/builds/" + buildNum
                        + "/downloads/" + fileName;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> getNeoForgeInstallerUrl(String version) {
        // https://maven.neoforged.net/releases/net/neoforged/neoforge/{version}/neoforge-{version}-installer.jar
        return CompletableFuture.completedFuture(
                "https://maven.neoforged.net/releases/net/neoforged/neoforge/" + version + "/neoforge-" + version
                        + "-installer.jar");
    }
}
