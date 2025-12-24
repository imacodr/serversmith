package dev.perillo.serversmith.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertiesService {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesService.class);
    private final Path filePath;
    private final List<String> lines = new ArrayList<>();
    private final Map<String, Integer> keyLineIndex = new HashMap<>();

    public PropertiesService(Path filePath) {
        this.filePath = filePath;
        if (Files.exists(filePath)) {
            load();
        }
    }

    public void load() {
        try {
            lines.clear();
            keyLineIndex.clear();
            List<String> fileLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            for (int i = 0; i < fileLines.size(); i++) {
                String line = fileLines.get(i);
                lines.add(line);
                parseLine(line, i);
            }
        } catch (IOException e) {
            logger.error("Failed to load properties: " + filePath, e);
        }
    }

    private void parseLine(String line, int index) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        int equalsIndex = trimmed.indexOf('=');
        if (equalsIndex > 0) {
            String key = trimmed.substring(0, equalsIndex).trim();
            keyLineIndex.put(key, index);
        }
    }

    public String getProperty(String key) {
        Integer index = keyLineIndex.get(key);
        if (index != null) {
            String line = lines.get(index);
            int equalsIndex = line.indexOf('=');
            if (equalsIndex > 0 && equalsIndex + 1 < line.length()) {
                return line.substring(equalsIndex + 1).trim();
            } else if (equalsIndex > 0) {
                return ""; // Empty value
            }
        }
        return null; // Not found
    }

    public void setProperty(String key, String value) {
        Integer index = keyLineIndex.get(key);
        String newLine = key + "=" + value;
        if (index != null) {
            lines.set(index, newLine);
        } else {
            lines.add(newLine);
            keyLineIndex.put(key, lines.size() - 1);
        }
    }

    public void save() {
        try {
            Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.error("Failed to save properties to " + filePath, e);
        }
    }
}
