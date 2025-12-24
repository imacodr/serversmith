package dev.perillo.serversmith.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
    public static final Path APP_DIR = Paths.get(System.getProperty("user.home"), ".serversmith");
    public static final Path CACHE_DIR = APP_DIR.resolve("cache");
    public static final Path CONFIG_FILE = APP_DIR.resolve("config.json");

    public static Path getInstanceMetadataPath(Path instanceDir) {
        return instanceDir.resolve(".serversmith").resolve("instance.json");
    }
}
