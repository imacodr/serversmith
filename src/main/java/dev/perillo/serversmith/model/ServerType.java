package dev.perillo.serversmith.model;

public enum ServerType {
    VANILLA("Vanilla"),
    PAPER("Paper"),
    NEOFORGE("NeoForge"),
    CUSTOM("Custom");

    private final String displayName;

    ServerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
