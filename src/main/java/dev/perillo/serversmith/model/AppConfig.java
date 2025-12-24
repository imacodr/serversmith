package dev.perillo.serversmith.model;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class AppConfig {
    private String defaultBaseDirectory;
    private Set<String> knownInstances = new HashSet<>();

    public AppConfig() {
    }

    public String getDefaultBaseDirectory() {
        return defaultBaseDirectory;
    }

    public void setDefaultBaseDirectory(String defaultBaseDirectory) {
        this.defaultBaseDirectory = defaultBaseDirectory;
    }

    public Set<String> getKnownInstances() {
        return knownInstances;
    }

    public void setKnownInstances(Set<String> knownInstances) {
        this.knownInstances = knownInstances;
    }
}
