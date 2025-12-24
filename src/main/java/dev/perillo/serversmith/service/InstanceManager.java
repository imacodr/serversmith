package dev.perillo.serversmith.service;

import dev.perillo.serversmith.model.AppConfig;
import dev.perillo.serversmith.model.ServerInstance;
import dev.perillo.serversmith.util.FileUtil;
import dev.perillo.serversmith.util.JsonUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class InstanceManager {
    private static final Logger logger = LoggerFactory.getLogger(InstanceManager.class);
    private final ObservableList<ServerInstance> instances = FXCollections.observableArrayList();
    private AppConfig config;

    public InstanceManager() {
        loadConfig();
        scanInstances();
    }

    private void loadConfig() {
        try {
            if (Files.exists(FileUtil.CONFIG_FILE)) {
                config = JsonUtil.load(FileUtil.CONFIG_FILE, AppConfig.class);
            } else {
                config = new AppConfig();
                config.setKnownInstances(new HashSet<>());
            }
        } catch (Exception e) {
            logger.error("Failed to load config", e);
            config = new AppConfig();
            config.setKnownInstances(new HashSet<>());
        }
    }

    public void saveConfig() {
        try {
            JsonUtil.save(FileUtil.CONFIG_FILE, config);
        } catch (IOException e) {
            logger.error("Failed to save config", e);
        }
    }

    private void scanInstances() {
        instances.clear();
        if (config.getKnownInstances() == null)
            return;

        Iterator<String> iterator = config.getKnownInstances().iterator();
        while (iterator.hasNext()) {
            String pathStr = iterator.next();
            Path path = Paths.get(pathStr);
            if (!Files.exists(path)) {
                // Keep it? Or remove it? Maybe user unplugged a drive.
                // Let's keep it for now but mark valid? Or just don't load.
                // For now, let's skip loading.
                continue;
            }

            Path metaPath = FileUtil.getInstanceMetadataPath(path);
            if (Files.exists(metaPath)) {
                try {
                    ServerInstance instance = JsonUtil.load(metaPath, ServerInstance.class);
                    instance.setLocation(path);
                    instances.add(instance);
                } catch (Exception e) {
                    logger.error("Failed to load instance metadata from " + path, e);
                }
            }
        }
    }

    public void addInstance(ServerInstance instance) {
        if (!config.getKnownInstances().contains(instance.getLocation().toAbsolutePath().toString())) {
            config.getKnownInstances().add(instance.getLocation().toAbsolutePath().toString());
            saveConfig();
        }
        instances.add(instance);
        saveInstanceMetadata(instance);
    }

    public void removeInstance(ServerInstance instance) {
        config.getKnownInstances().remove(instance.getLocation().toAbsolutePath().toString());
        saveConfig();
        instances.remove(instance);
    }

    public void deleteInstance(ServerInstance instance) {
        removeInstance(instance);
        try {
            // Recursive delete
            Files.walk(instance.getLocation())
                    .sorted((a, b) -> b.compareTo(a)) // Delete children first
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            logger.error("Failed to delete " + p, e);
                        }
                    });
        } catch (IOException e) {
            logger.error("Failed to delete instance directory " + instance.getName(), e);
        }
    }

    public void saveInstanceMetadata(ServerInstance instance) {
        try {
            Path metaPath = FileUtil.getInstanceMetadataPath(instance.getLocation());
            JsonUtil.save(metaPath, instance);
        } catch (IOException e) {
            logger.error("Failed to save instance metadata for " + instance.getName(), e);
        }
    }

    public ObservableList<ServerInstance> getInstances() {
        return instances;
    }

    public AppConfig getConfig() {
        return config;
    }
}
