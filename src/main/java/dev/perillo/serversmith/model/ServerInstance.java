package dev.perillo.serversmith.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javafx.beans.property.*;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class ServerInstance {
    private final StringProperty name = new SimpleStringProperty();
    private ServerType type;
    private final StringProperty version = new SimpleStringProperty();
    private LocalDateTime created;
    private LocalDateTime lastOpened;

    // Not serialized, runtime only
    @JsonIgnore
    private Path location;
    @JsonIgnore
    private final BooleanProperty running = new SimpleBooleanProperty(false);

    public ServerInstance() {
    }

    public ServerInstance(String name, ServerType type, String version, Path location) {
        setName(name);
        this.type = type;
        setVersion(version);
        this.location = location;
        this.created = LocalDateTime.now();
        this.lastOpened = LocalDateTime.now();
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public ServerType getType() {
        return type;
    }

    public void setType(ServerType type) {
        this.type = type;
    }

    public String getVersion() {
        return version.get();
    }

    public void setVersion(String version) {
        this.version.set(version);
    }

    public StringProperty versionProperty() {
        return version;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getLastOpened() {
        return lastOpened;
    }

    public void setLastOpened(LocalDateTime lastOpened) {
        this.lastOpened = lastOpened;
    }

    public Path getLocation() {
        return location;
    }

    public void setLocation(Path location) {
        this.location = location;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean running) {
        this.running.set(running);
    }

    public BooleanProperty runningProperty() {
        return running;
    }

    private int minMemory = 1024;
    private int maxMemory = 2048;

    public int getMinMemory() {
        return minMemory;
    }

    public void setMinMemory(int minMemory) {
        this.minMemory = minMemory;
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }
}
