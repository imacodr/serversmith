package dev.perillo.serversmith.ui;

import dev.perillo.serversmith.model.ServerInstance;
import dev.perillo.serversmith.model.ServerType;
import dev.perillo.serversmith.service.DownloadService;
import dev.perillo.serversmith.service.InstanceManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class WizardDialog extends Dialog<ServerInstance> {
    private final InstanceManager instanceManager;
    private final DownloadService downloadService = new DownloadService();

    // Data State
    private ServerType selectedType = ServerType.VANILLA;
    private String selectedVersion;
    private String serverName = "My Server";
    private Path serverPath;
    private boolean eulaAccepted = false;

    // UI Components
    private VBox contentArea;
    private Button nextButton;
    private Button backButton;
    private Label stepTitle;
    private Label stepDescription;
    private final int totalSteps = 3;

    public WizardDialog(InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
        setTitle("ServerSmith Setup");
        setResizable(true);

        // Set default path
        String defInfo = instanceManager.getConfig().getDefaultBaseDirectory();
        serverPath = (defInfo != null) ? Paths.get(defInfo) : Paths.get(System.getProperty("user.home"));

        DialogPane pane = getDialogPane();
        pane.getStylesheets().add(getClass().getResource("/dev/perillo/serversmith/styles.css").toExternalForm());
        pane.setMinWidth(850);
        pane.setMinHeight(650);

        // We manage our own buttons mostly
        pane.getButtonTypes().add(ButtonType.CANCEL);
        Node cancelBtn = pane.lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null)
            cancelBtn.setVisible(false); // Hide the default button

        // --- Main Layout ---
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: -color-background;");

        // --- Header Section ---
        HBox header = new HBox();
        header.setPadding(new Insets(20, 25, 20, 25));
        header.setStyle("-fx-background-color: -color-surface;");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox headerText = new VBox(5);
        stepTitle = new Label("Select Platform");
        stepTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-on-surface;");

        stepDescription = new Label("Choose the type of server you want to create.");
        stepDescription.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-on-surface; -fx-opacity: 0.7;");

        headerText.getChildren().addAll(stepTitle, stepDescription);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        // Small branding icon in header
        javafx.scene.shape.SVGPath brandIcon = Icons.getVanilla();
        brandIcon.setScaleX(0.8);
        brandIcon.setScaleY(0.8);
        brandIcon.setFill(javafx.scene.paint.Color.web("#f97316"));

        header.getChildren().addAll(headerText, brandIcon);

        // --- Content Area ---
        contentArea = new VBox(25); // Increased spacing
        contentArea.setPadding(new Insets(40, 50, 40, 50)); // Increased padding
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // --- Footer Section ---
        VBox footerRoot = new VBox(0);
        Separator footerSep = new Separator();

        HBox footer = new HBox(12);
        footer.setPadding(new Insets(20, 30, 20, 30));
        footer.setAlignment(Pos.CENTER_RIGHT);

        backButton = new Button("< Back");
        backButton.setMinWidth(80);
        backButton.setOnAction(e -> prevStep());

        nextButton = new Button("Next >");
        nextButton.setMinWidth(80);
        nextButton.setDefaultButton(true);
        nextButton.setOnAction(e -> nextStep());

        Button cancelBtnManual = new Button("Cancel");
        cancelBtnManual.setMinWidth(80);
        cancelBtnManual.setOnAction(e -> close());

        footer.getChildren().addAll(backButton, nextButton, new Region() {
            {
                setMinWidth(10);
            }
        }, cancelBtnManual);

        footerRoot.getChildren().addAll(footerSep, footer);

        root.getChildren().addAll(header, new Separator(), contentArea, footerRoot);
        pane.setContent(root);

        loadStep(1);
    }

    private void nextStep() {
        if (currentStep == 1) {
            if (selectedVersion == null)
                return; // Validate
            loadStep(2);
        } else if (currentStep == 2) {
            if (serverName.isEmpty() || !eulaAccepted)
                return; // Validate
            // Pass a callback to run after the step is fully loaded (and animated in)
            loadStep(3, this::startInstallation);
        }
    }

    private void prevStep() {
        if (currentStep > 1) {
            loadStep(currentStep - 1, null);
        }
    }

    private void loadStep(int step) {
        loadStep(step, null);
    }

    private void loadStep(int step, Runnable onComplete) {
        // Fade out current content
        if (!contentArea.getChildren().isEmpty()) {
            FadeTransition ft = new FadeTransition(Duration.millis(150), contentArea);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> {
                loadStepContent(step);
                // Fade in new content
                FadeTransition ftIn = new FadeTransition(Duration.millis(150), contentArea);
                ftIn.setFromValue(0.0);
                ftIn.setToValue(1.0);
                if (onComplete != null) {
                    ftIn.setOnFinished(ev -> onComplete.run());
                }
                ftIn.play();
            });
            ft.play();
        } else {
            loadStepContent(step);
            if (onComplete != null) {
                // Run immediately if no transition (e.g. first load)
                // checking onComplete again inside just to be safe, though unexpected for step
                // 1
                onComplete.run();
            }
        }
    }

    private int currentStep = 1;

    private void loadStepContent(int step) {
        currentStep = step;
        contentArea.getChildren().clear();

        switch (step) {
            case 1:
                setupPlatformStep();
                break;
            case 2:
                setupConfigStep();
                break;
            case 3:
                setupInstallStep();
                break;
        }
    }

    // ... existing navigation methods ...

    // --- STEP 1: Platform ---
    private ComboBox<String> versionCombo;

    private void setupPlatformStep() {
        stepTitle.setText("Step 1: Select Platform");
        stepDescription.setText("Choose the type of server you want to create and select a version.");
        backButton.setDisable(true);
        nextButton.setText("Next >");
        nextButton.setDisable(selectedVersion == null);

        GridPane typesGrid = new GridPane();
        typesGrid.setHgap(30); // Increased gap
        typesGrid.setVgap(30);
        typesGrid.setAlignment(Pos.CENTER);

        RowConstraints row = new RowConstraints();
        row.setValignment(javafx.geometry.VPos.CENTER);
        row.setFillHeight(false);
        typesGrid.getRowConstraints().add(row);

        // Cards for types
        ToggleGroup group = new ToggleGroup();

        Node cardVanilla = createTypeCard(ServerType.VANILLA, "Official Minecraft Server", group);
        Node cardPaper = createTypeCard(ServerType.PAPER, "High Performance Fork", group);
        Node cardNeo = createTypeCard(ServerType.NEOFORGE, "Modern Mod Loader", group);

        typesGrid.add(cardVanilla, 0, 0);
        typesGrid.add(cardPaper, 1, 0);
        typesGrid.add(cardNeo, 2, 0);

        // Version Selection
        VBox versionBox = new VBox(5);
        versionBox.setMaxWidth(300);
        versionBox.setAlignment(Pos.CENTER);

        Label verLabel = new Label("Game Version");
        versionCombo = new ComboBox<>();
        versionCombo.setMaxWidth(Double.MAX_VALUE);
        versionCombo.setOnAction(e -> {
            selectedVersion = versionCombo.getValue();
            nextButton.setDisable(selectedVersion == null);
        });

        versionBox.getChildren().addAll(verLabel, versionCombo);

        contentArea.setAlignment(Pos.CENTER); // Center everything for this step

        // Add some extra space around the separator
        Region spacer1 = new Region();
        spacer1.setMinHeight(20);
        Region spacer2 = new Region();
        spacer2.setMinHeight(10);

        contentArea.getChildren().addAll(typesGrid, spacer1, new Separator(), spacer2, versionBox);

        // Trigger initial load
        if (selectedType != null) {
            loadVersionsFor(selectedType);
        }
    }

    private Node createTypeCard(ServerType type, String subtitle, ToggleGroup group) {
        ToggleButton btn = new ToggleButton();
        btn.setToggleGroup(group);
        btn.setSelected(type == selectedType);
        btn.setUserData(type);

        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));

        Node icon;
        if (type == ServerType.VANILLA)
            icon = Icons.getVanilla();
        else if (type == ServerType.PAPER)
            icon = Icons.getPaper();
        else
            icon = Icons.getNeoForge();

        Label title = new Label(type.name());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label sub = new Label(subtitle);
        sub.setWrapText(true);
        sub.setStyle("-fx-font-size: 11px; -fx-opacity: 0.7;");
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        box.getChildren().addAll(icon, title, sub);
        btn.setGraphic(box);

        // Style the toggle button to look like a card
        btn.getStyleClass().add("card-toggle");

        // Ensure even sizing and rectangular shape
        btn.setMinWidth(180);
        btn.setPrefWidth(180);
        btn.setMaxWidth(180);
        btn.setMinHeight(240);
        btn.setPrefHeight(240);
        btn.setMaxHeight(240);

        // Animations
        Icons.animateHover(btn);

        btn.selectedProperty().addListener((obs, old, val) -> {
            if (val) {
                selectedType = type;
                loadVersionsFor(type);
                Icons.animateClick(btn);
            }
        });

        return btn;
    }

    private void loadVersionsFor(ServerType type) {
        versionCombo.getItems().clear();
        versionCombo.setPromptText("Loading...");
        versionCombo.setDisable(true);
        nextButton.setDisable(true);

        CompletableFuture<java.util.List<String>> future;
        if (type == ServerType.VANILLA)
            future = downloadService.fetchVanillaVersions();
        else if (type == ServerType.PAPER)
            future = downloadService.fetchPaperVersions();
        else if (type == ServerType.NEOFORGE)
            future = downloadService.fetchNeoForgeVersions();
        else {
            // Custom
            versionCombo.getItems().add("Custom");
            versionCombo.setValue("Custom");
            versionCombo.setDisable(false);
            nextButton.setDisable(false);
            return;
        }

        future.thenAccept(vers -> Platform.runLater(() -> {
            versionCombo.getItems().setAll(vers);
            versionCombo.setDisable(false);
            if (!vers.isEmpty()) {
                versionCombo.setValue(vers.get(0));
                selectedVersion = vers.get(0);
                nextButton.setDisable(false);
            } else {
                versionCombo.setPromptText("No versions found");
            }
        }));
    }

    // --- STEP 2: Config ---
    private TextField pathField;
    private CheckBox eulaCheck;
    private TextField nameField;

    private void setupConfigStep() {
        stepTitle.setText("Step 2: Configuration");
        stepDescription.setText("Give your server a name and choose the installation directory.");
        backButton.setDisable(false);
        nextButton.setText("Install"); // Action changes to Install
        // nextButton.setDisable(!eulaAccepted || serverName.isEmpty());

        contentArea.setAlignment(Pos.TOP_LEFT);
        contentArea.setSpacing(35); // Increased from 20
        contentArea.setPadding(new Insets(40, 60, 40, 60)); // Extra padding for config

        GridPane grid = new GridPane();
        grid.setVgap(25); // Increased from 15
        grid.setHgap(15);

        nameField = new TextField(serverName);
        nameField.textProperty().addListener((o, old, val) -> {
            serverName = val;
            validateStep2();
        });

        pathField = new TextField(serverPath.toString());
        pathField.setEditable(false);
        Button browseBtn = new Button("Browse");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(getDialogPane().getScene().getWindow());
            if (f != null) {
                serverPath = f.toPath();
                pathField.setText(f.getAbsolutePath());
            }
        });

        eulaCheck = new CheckBox("I agree to the Minecraft EULA");
        eulaCheck.setSelected(eulaAccepted);
        eulaCheck.selectedProperty().addListener((o, old, val) -> {
            eulaAccepted = val;
            validateStep2();
        });

        grid.add(new Label("Server Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Location:"), 0, 1);
        HBox pathBox = new HBox(5, pathField, browseBtn);
        HBox.setHgrow(pathField, Priority.ALWAYS);
        grid.add(pathBox, 1, 1);

        grid.add(new Label("Legal:"), 0, 2);
        grid.add(eulaCheck, 1, 2);

        VBox summary = new VBox(5);
        summary.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-background-radius: 4;");
        summary.getChildren().addAll(
                new Label("Summary:"),
                new Label("Type: " + selectedType),
                new Label("Version: " + selectedVersion));

        contentArea.getChildren().addAll(summary, new Separator(), grid);
        validateStep2();
    }

    private void validateStep2() {
        boolean valid = !serverName.trim().isEmpty() && eulaAccepted;
        nextButton.setDisable(!valid);
    }

    // --- STEP 3: Install ---
    private ProgressBar progressBar;
    private Label statusLabel;
    private TextArea logArea;

    private void setupInstallStep() {
        stepTitle.setText("Step 3: Installation");
        stepDescription.setText("Please wait while ServerSmith downloads and prepares your server.");
        backButton.setDisable(true); // Can't go back once started
        nextButton.setVisible(false); // Hide until done

        contentArea.setAlignment(Pos.TOP_LEFT);
        contentArea.setSpacing(25); // Increased from 15
        contentArea.setPadding(new Insets(40, 60, 40, 60));

        statusLabel = new Label("Initializing...");
        statusLabel.setStyle("-fx-font-weight: bold;");

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        contentArea.getChildren().addAll(statusLabel, progressBar, logArea);
    }

    private void appendLog(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    private void startInstallation() {
        Path instanceDir = serverPath.resolve(serverName);

        if (Files.exists(instanceDir)) {
            statusLabel.setText("Error: Directory already exists!");
            appendLog("Error: Target directory " + instanceDir + " already exists.");
            return;
        }

        new Thread(() -> {
            try {
                appendLog("Creating directory: " + instanceDir);
                Files.createDirectories(instanceDir);

                if (eulaAccepted) {
                    Files.writeString(instanceDir.resolve("eula.txt"), "eula=true\n");
                    appendLog("Accepted EULA.");
                }

                String url = null;
                boolean isInstaller = false;

                appendLog("Fetching download URL for " + selectedType + " " + selectedVersion + "...");

                if (selectedType == ServerType.VANILLA) {
                    url = downloadService.getVanillaDownloadUrl(selectedVersion).join();
                } else if (selectedType == ServerType.PAPER) {
                    url = downloadService.getPaperDownloadUrl(selectedVersion).join();
                } else if (selectedType == ServerType.NEOFORGE) {
                    url = downloadService.getNeoForgeInstallerUrl(selectedVersion).join();
                    isInstaller = true;
                }

                if (url == null) {
                    throw new Exception("Could not find download URL.");
                }

                appendLog("Downloading jar from " + url);
                Platform.runLater(() -> statusLabel.setText("Downloading..."));

                Path targetJar = instanceDir.resolve(isInstaller ? "installer.jar" : "server.jar");
                downloadService.downloadFile(url, targetJar, progress -> {
                    Platform.runLater(() -> progressBar.setProgress(progress));
                }).join();

                appendLog("Download complete.");

                if (isInstaller && selectedType == ServerType.NEOFORGE) {
                    Platform.runLater(() -> statusLabel.setText("Running Installer... (This may take a while)"));
                    appendLog("Launching NeoForge installer...");

                    ProcessBuilder pb = new ProcessBuilder("java", "-jar", "installer.jar", "--installServer");
                    pb.directory(instanceDir.toFile());
                    pb.redirectErrorStream(true);
                    Process p = pb.start();

                    // Read installer output?
                    try (java.util.Scanner s = new java.util.Scanner(p.getInputStream())) {
                        while (s.hasNextLine()) {
                            String line = s.nextLine();
                            // appendLog("[Installer] " + line); // might be too verbose
                        }
                    }

                    int exit = p.waitFor();
                    if (exit != 0)
                        throw new Exception("Installer failed with code " + exit);
                    appendLog("Installer finished successfully.");
                }

                ServerInstance instance = new ServerInstance(serverName, selectedType, selectedVersion, instanceDir);

                Platform.runLater(() -> {
                    statusLabel.setText("Success!");
                    progressBar.setProgress(1.0);
                    instanceManager.addInstance(instance);

                    // Transformation to "Done" button
                    nextButton.setText("Finish");
                    nextButton.setOnAction(e -> {
                        setResult(instance);
                        close();
                    });
                    nextButton.setDisable(false);
                    nextButton.setVisible(true);
                    nextButton.requestFocus();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Error!");
                    statusLabel.setStyle("-fx-text-fill: -color-error;");
                    appendLog("FAILED: " + e.getMessage());
                    // Allow cancel?
                });
            }
        }).start();
    }
}
