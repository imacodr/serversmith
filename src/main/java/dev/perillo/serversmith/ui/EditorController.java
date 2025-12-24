package dev.perillo.serversmith.ui;

import dev.perillo.serversmith.model.ServerInstance;
import dev.perillo.serversmith.service.ProcessService;
import dev.perillo.serversmith.service.PropertiesService;
import dev.perillo.serversmith.util.FileUtil;
import dev.perillo.serversmith.util.ImageUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.beans.binding.Bindings;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EditorController {
    private final BorderPane root = new BorderPane();
    private final ProcessService processService;
    private ServerInstance currentInstance;

    private TabPane tabPane;
    private TextArea consoleArea;
    private Button startBtn, stopBtn;

    // Properties controls
    private TextField portField;
    private TextField maxPlayersField;
    private CheckBox onlineModeCheck;
    private ComboBox<String> difficultyCombo;
    private ComboBox<String> gamemodeCombo;
    private CheckBox pvpCheck;
    private CheckBox whitelistCheck;
    private TextArea motdArea;

    // Icon
    private ImageView iconView;

    public EditorController(ProcessService processService) {
        this.processService = processService;
        initialize();
    }

    private void initialize() {
        tabPane = new TabPane();
        root.setCenter(createWelcomeView());
    }

    public Parent getView() {
        return root;
    }

    public void setServer(ServerInstance instance) {
        this.currentInstance = instance;
        if (instance == null) {
            root.setCenter(createWelcomeView());
            return;
        }

        tabPane.getTabs().clear();
        tabPane.getTabs().addAll(
                createDashboardTab(),
                createSettingsTab(),
                createFilesTab());
        root.setCenter(tabPane);

        // Refresh Icon
        loadIcon();
    }

    private Parent createWelcomeView() {
        VBox box = new VBox(20);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setPadding(new Insets(40));

        Label title = new Label("ServerSmith"); // Branding placeholder
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: -color-primary;");

        Label subtitle = new Label(
                "Select a server from the sidebar to manage it,\nor create a new one to get started.");
        subtitle.setStyle("-fx-font-size: 18px; -fx-text-fill: -color-on-surface; -fx-opacity: 0.7;");

        // You could add a logo ImageView here later

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private Tab createDashboardTab() {
        Tab tab = new Tab("Dashboard");
        tab.setClosable(false);

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: -color-background;");

        // --- Hero Card: Status & Controls ---
        HBox heroCard = new HBox(20);
        heroCard.getStyleClass().add("card");
        heroCard.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        heroCard.setPadding(new Insets(20));

        // Left: Info
        VBox infoBox = new VBox(5);
        Label serverTitle = new Label(currentInstance.getName());
        serverTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -color-on-surface;");

        Label versionSubtitle = new Label(currentInstance.getType() + " " + currentInstance.getVersion());
        versionSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-on-surface; -fx-opacity: 0.7;");

        // Status Badge
        Label statusBadge = new Label();
        statusBadge.getStyleClass().add("status-badge");

        // Complex binding for style and text
        statusBadge.textProperty().bind(Bindings.when(currentInstance.runningProperty())
                .then("RUNNING")
                .otherwise("STOPPED"));

        statusBadge.styleProperty().bind(Bindings.when(currentInstance.runningProperty())
                .then("-fx-background-color: rgba(76, 175, 80, 0.2); -fx-text-fill: #4caf50; -fx-border-color: #4caf50; -fx-border-radius: 12;") // Manual
                                                                                                                                                 // inline
                                                                                                                                                 // for
                                                                                                                                                 // now
                                                                                                                                                 // or
                                                                                                                                                 // toggle
                                                                                                                                                 // class
                                                                                                                                                 // listener
                .otherwise(
                        "-fx-background-color: rgba(244, 67, 54, 0.2); -fx-text-fill: #f44336; -fx-border-color: #f44336; -fx-border-radius: 12;"));

        // Listeners for class toggling is cleaner but binding style is easier inline
        // for 1 file edits
        // Let's use the CSS classes we made by adding a listener
        currentInstance.runningProperty().addListener((obs, old, isRunning) -> {
            statusBadge.getStyleClass().removeAll("running", "stopped");
            statusBadge.getStyleClass().add(isRunning ? "running" : "stopped");
        });
        // Initial state
        statusBadge.getStyleClass().add(currentInstance.isRunning() ? "running" : "stopped");

        HBox titleBox = new HBox(10, serverTitle, statusBadge);
        titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        infoBox.getChildren().addAll(titleBox, versionSubtitle);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Right: Actions
        startBtn = new Button("Start");
        startBtn.setGraphic(Icons.getPlay());
        startBtn.getStyleClass().add("action-button");
        startBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;"); // Green for start
        Icons.animateHover(startBtn);

        stopBtn = new Button("Stop");
        stopBtn.setGraphic(Icons.getStop());
        stopBtn.getStyleClass().add("action-button");
        stopBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;"); // Red for stop
        Icons.animateHover(stopBtn);

        // Bind buttons
        startBtn.disableProperty().bind(currentInstance.runningProperty());
        stopBtn.disableProperty().bind(currentInstance.runningProperty().not());

        startBtn.setOnAction(e -> startServer());
        stopBtn.setOnAction(e -> processService.stopServer(currentInstance));

        heroCard.getChildren().addAll(infoBox, startBtn, stopBtn);

        // --- Terminal Section ---
        VBox terminalContainer = new VBox(0);
        terminalContainer.getStyleClass().add("card");
        terminalContainer.setPadding(Insets.EMPTY); // Terminal fills card
        VBox.setVgrow(terminalContainer, Priority.ALWAYS);

        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.getStyleClass().add("terminal-area");
        consoleArea.setWrapText(true);
        VBox.setVgrow(consoleArea, Priority.ALWAYS);

        TextField commandField = new TextField();
        commandField.setPromptText("Enter server command...");
        commandField.setStyle(
                "-fx-background-radius: 0 0 4 4; -fx-border-width: 1 0 0 0; -fx-border-color: #333; -fx-background-color: #1e1e1e; -fx-text-fill: white; -fx-font-family: 'monospaced';");
        commandField.setOnAction(e -> {
            String cmd = commandField.getText();
            if (!cmd.trim().isEmpty() && currentInstance.isRunning()) {
                consoleArea.appendText("> " + cmd + "\n");
                processService.sendCommand(currentInstance, cmd);
                commandField.clear();
            }
        });

        terminalContainer.getChildren().addAll(consoleArea, commandField);

        content.getChildren().addAll(heroCard, terminalContainer);

        tab.setContent(content);
        return tab;
    }

    private Tab createSettingsTab() {
        Tab tab = new Tab("Settings");
        tab.setClosable(false);

        // Load properties
        Path propsFile = currentInstance.getLocation().resolve("server.properties");
        PropertiesService props = new PropertiesService(propsFile);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: -color-background; -fx-border-color: transparent;");

        VBox rootContainer = new VBox(20);
        rootContainer.setPadding(new Insets(20));
        rootContainer.setStyle("-fx-background-color: -color-background;");

        // --- Card: Network Settings ---
        GridPane netGrid = new GridPane();
        netGrid.setHgap(15);
        netGrid.setVgap(15);

        portField = new TextField(props.getProperty("server-port"));
        onlineModeCheck = new CheckBox("Online Mode");
        onlineModeCheck.setSelected(Boolean.parseBoolean(props.getProperty("online-mode")));

        netGrid.add(new Label("Server Port:"), 0, 0);
        netGrid.add(portField, 1, 0);
        netGrid.add(new Label("Security:"), 0, 1);
        netGrid.add(onlineModeCheck, 1, 1);

        VBox networkCard = new VBox(10, new Label("Network"), netGrid);
        networkCard.getStyleClass().add("card");

        // --- Card: Gameplay Settings ---
        GridPane gameGrid = new GridPane();
        gameGrid.setHgap(15);
        gameGrid.setVgap(15);

        maxPlayersField = new TextField(props.getProperty("max-players"));
        pvpCheck = new CheckBox("Enable PvP");
        pvpCheck.setSelected(Boolean.parseBoolean(props.getProperty("pvp")));
        whitelistCheck = new CheckBox("Whitelist Enabled");
        whitelistCheck.setSelected(Boolean.parseBoolean(props.getProperty("white-list")));

        difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll("peaceful", "easy", "normal", "hard");
        difficultyCombo.setValue(props.getProperty("difficulty"));

        gamemodeCombo = new ComboBox<>();
        gamemodeCombo.getItems().addAll("survival", "creative", "adventure", "spectator");
        gamemodeCombo.setValue(props.getProperty("gamemode"));

        gameGrid.add(new Label("Max Players:"), 0, 0);
        gameGrid.add(maxPlayersField, 1, 0);
        gameGrid.add(new Label("Gamemode:"), 0, 1);
        gameGrid.add(gamemodeCombo, 1, 1);
        gameGrid.add(new Label("Difficulty:"), 0, 2);
        gameGrid.add(difficultyCombo, 1, 2);
        gameGrid.add(new Label("Features:"), 0, 3);
        HBox toggles = new HBox(15, pvpCheck, whitelistCheck);
        gameGrid.add(toggles, 1, 3);

        VBox gameplayCard = new VBox(10, new Label("Gameplay"), gameGrid);
        gameplayCard.getStyleClass().add("card");

        // --- Card: MOTD ---
        motdArea = new TextArea(props.getProperty("motd"));
        motdArea.setPrefRowCount(3);

        FlowPane motdToolBar = new FlowPane(5, 5); // Use FlowPane for wrapping
        String[] codes = { "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e",
                "&f", "&k", "&l", "&m", "&n", "&o", "&r" };
        for (String code : codes) {
            Button b = new Button(code);
            String style = "-fx-font-size: 0.9em; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-family: 'monospaced'; -fx-font-weight: bold; ";
            switch (code) {
                case "&0":
                    style += "-fx-background-color: #000000; -fx-text-fill: #ffffff;";
                    break;
                case "&1":
                    style += "-fx-background-color: #0000AA; -fx-text-fill: #ffffff;";
                    break;
                case "&2":
                    style += "-fx-background-color: #00AA00; -fx-text-fill: #ffffff;";
                    break;
                case "&3":
                    style += "-fx-background-color: #00AAAA; -fx-text-fill: #ffffff;";
                    break;
                case "&4":
                    style += "-fx-background-color: #AA0000; -fx-text-fill: #ffffff;";
                    break;
                case "&5":
                    style += "-fx-background-color: #AA00AA; -fx-text-fill: #ffffff;";
                    break;
                case "&6":
                    style += "-fx-background-color: #FFAA00; -fx-text-fill: #000000;";
                    break;
                case "&7":
                    style += "-fx-background-color: #AAAAAA; -fx-text-fill: #000000;";
                    break;
                case "&8":
                    style += "-fx-background-color: #555555; -fx-text-fill: #ffffff;";
                    break;
                case "&9":
                    style += "-fx-background-color: #5555FF; -fx-text-fill: #ffffff;";
                    break;
                case "&a":
                    style += "-fx-background-color: #55FF55; -fx-text-fill: #000000;";
                    break;
                case "&b":
                    style += "-fx-background-color: #55FFFF; -fx-text-fill: #000000;";
                    break;
                case "&c":
                    style += "-fx-background-color: #FF5555; -fx-text-fill: #000000;";
                    break;
                case "&d":
                    style += "-fx-background-color: #FF55FF; -fx-text-fill: #000000;";
                    break;
                case "&e":
                    style += "-fx-background-color: #FFFF55; -fx-text-fill: #000000;";
                    break;
                case "&f":
                    style += "-fx-background-color: #FFFFFF; -fx-text-fill: #000000;";
                    break;
                case "&l":
                    style += "-fx-font-weight: bold;";
                    break;
                case "&n":
                    style += "-fx-underline: true;";
                    break;
                case "&o":
                    style += "-fx-font-style: italic;";
                    break;
                case "&m":
                    style += "-fx-strikethrough: true;";
                    break;
                case "&r":
                    style += "-fx-text-fill: -color-error;";
                    break;
                default:
                    break;
            }
            b.setStyle(style);
            b.setOnAction(e -> motdArea.appendText(code));
            Icons.animateHover(b);
            motdToolBar.getChildren().add(b);
        }
        VBox motdCard = new VBox(10, new Label("Message of the Day"), motdToolBar, motdArea);
        motdCard.getStyleClass().add("card");

        // --- Card: Icon ---
        HBox iconBox = new HBox(15);
        iconBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        iconView = new ImageView();
        iconView.setFitWidth(64);
        iconView.setFitHeight(64);
        Button uploadIconBtn = new Button("Change Icon");
        uploadIconBtn.setOnAction(e -> chooseIcon());
        iconBox.getChildren().addAll(iconView, uploadIconBtn);

        VBox iconCard = new VBox(10, new Label("Server Icon"), iconBox);
        iconCard.getStyleClass().add("card");

        // --- Save Action ---
        Button saveBtn = new Button("Save Changes");
        saveBtn.setPrefWidth(150);
        saveBtn.setOnAction(e -> {
            props.setProperty("server-port", portField.getText());
            props.setProperty("max-players", maxPlayersField.getText());
            props.setProperty("online-mode", String.valueOf(onlineModeCheck.isSelected()));
            props.setProperty("pvp", String.valueOf(pvpCheck.isSelected()));
            props.setProperty("white-list", String.valueOf(whitelistCheck.isSelected()));
            props.setProperty("difficulty", difficultyCombo.getValue());
            props.setProperty("gamemode", gamemodeCombo.getValue());
            props.setProperty("motd", motdArea.getText());
            props.save();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Settings Saved!");
            alert.show();
        });

        HBox actions = new HBox(saveBtn);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        // Assemble
        rootContainer.getChildren().addAll(networkCard, gameplayCard, motdCard, iconCard, actions);

        scroll.setContent(rootContainer);
        tab.setContent(scroll);
        return tab;
    }

    private Tab createFilesTab() {
        Tab tab = new Tab("Files");
        tab.setClosable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: -color-background;");

        // --- Toolbar ---
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button openFolderBtn = new Button("Open Folder");
        openFolderBtn.setGraphic(Icons.getFolder());
        openFolderBtn.setOnAction(e -> openFile(currentInstance.getLocation().toFile()));

        Button refreshBtn = new Button("Refresh");
        // Reuse some icon or just text for now
        refreshBtn.setOnAction(e -> refreshFileList());

        toolbar.getChildren().addAll(openFolderBtn, refreshBtn);

        // --- File List ---
        fileListView = new ListView<>();
        fileListView.getStyleClass().add("card");
        VBox.setVgrow(fileListView, Priority.ALWAYS);

        fileListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getFileName().toString());
                    boolean isDir = java.nio.file.Files.isDirectory(item);
                    setGraphic(isDir ? Icons.getFolder() : Icons.getFile());

                    // Style
                    setStyle("-fx-padding: 8 12;");
                }
            }
        });

        fileListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Path selected = fileListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openFile(selected.toFile());
                }
            }
        });

        root.getChildren().addAll(toolbar, fileListView);

        // Initial load
        refreshFileList();

        tab.setContent(root);
        return tab;
    }

    private ListView<Path> fileListView;

    private void refreshFileList() {
        if (fileListView == null || currentInstance == null)
            return;
        try (var stream = java.nio.file.Files.list(currentInstance.getLocation())) {
            fileListView.getItems().setAll(stream.sorted((p1, p2) -> {
                boolean d1 = java.nio.file.Files.isDirectory(p1);
                boolean d2 = java.nio.file.Files.isDirectory(p2);
                if (d1 && !d2)
                    return -1;
                if (!d1 && d2)
                    return 1;
                return p1.getFileName().toString().compareToIgnoreCase(p2.getFileName().toString());
            }).toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startServer() {
        try {
            consoleArea.clear();
            processService.startServer(currentInstance, currentInstance.getMinMemory(), currentInstance.getMaxMemory(),
                    line -> Platform.runLater(() -> consoleArea.appendText(line + "\n")));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to start server: " + e.getMessage()).show();
        }
    }

    private void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                if (file.isDirectory() || file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    new Alert(Alert.AlertType.WARNING, "File not found: " + file.getName()).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void chooseIcon() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.webp"));
        File file = fc.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            try {
                ImageUtil.saveServerIcon(file.toPath(), currentInstance.getLocation());
                loadIcon(); // refresh
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to update icon: " + e.getMessage()).show();
            }
        }
    }

    private void loadIcon() {
        Path iconPath = currentInstance.getLocation().resolve("server-icon.png");
        if (Files.exists(iconPath)) {
            try {
                iconView.setImage(new Image(iconPath.toUri().toString()));
            } catch (Exception e) {
                iconView.setImage(null);
            }
        } else {
            iconView.setImage(null);
        }
    }
}
