package dev.perillo.serversmith.ui;

import dev.perillo.serversmith.model.ServerInstance;
import dev.perillo.serversmith.service.InstanceManager;
import javafx.beans.binding.Bindings;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import java.util.function.Consumer;

public class SidebarController {
    private final VBox root = new VBox(10);
    private final InstanceManager instanceManager;
    private final Consumer<ServerInstance> onSelect;
    private final Runnable onCreate;
    private ListView<ServerInstance> listView;

    public SidebarController(InstanceManager instanceManager, Consumer<ServerInstance> onSelect, Runnable onCreate) {
        this.instanceManager = instanceManager;
        this.onSelect = onSelect;
        this.onCreate = onCreate;

        initialize();
    }

    private void initialize() {
        root.setPadding(new Insets(10));

        TextField searchField = new TextField();
        searchField.setPromptText("Search servers...");

        FilteredList<ServerInstance> filteredList = new FilteredList<>(instanceManager.getInstances(), p -> true);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredList.setPredicate(instance -> {
                if (newV == null || newV.isEmpty())
                    return true;
                return instance.getName().toLowerCase().contains(newV.toLowerCase());
            });
        });

        listView = new ListView<>(filteredList);
        listView.setCellFactory(param -> new ServerListCell());
        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, newV) -> {
            onSelect.accept(newV);
        });
        VBox.setVgrow(listView, Priority.ALWAYS);

        Button createBtn = new Button("Create Server");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.getStyleClass().add("action-button");
        Icons.animateHover(createBtn);
        createBtn.setOnAction(e -> onCreate.run());

        root.getChildren().addAll(searchField, listView, createBtn);
    }

    public Parent getView() {
        return root;
    }

    private class ServerListCell extends ListCell<ServerInstance> {
        @Override
        protected void updateItem(ServerInstance item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                getStyleClass().remove("selected");
            } else {
                HBox cellRoot = new HBox(12);
                cellRoot.setPadding(new Insets(8, 12, 8, 12));
                cellRoot.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                // Server Type Icon
                javafx.scene.shape.SVGPath typeIcon;
                switch (item.getType()) {
                    case VANILLA:
                        typeIcon = Icons.getSmallVanilla();
                        break;
                    case PAPER:
                        typeIcon = Icons.getSmallPaper();
                        break;
                    case NEOFORGE:
                        typeIcon = Icons.getSmallNeoForge();
                        break;
                    default:
                        typeIcon = Icons.getSmallVanilla();
                }
                typeIcon.setFill(Color.web("#f97316"));

                VBox iconContainer = new VBox(typeIcon);
                iconContainer.setMinWidth(24);
                iconContainer.setAlignment(javafx.geometry.Pos.CENTER);

                // Details
                VBox details = new VBox(2);
                Label nameLabel = new Label(item.getName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-on-surface;");

                Label versionLabel = new Label(item.getVersion());
                versionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-on-surface; -fx-opacity: 0.6;");

                details.getChildren().addAll(nameLabel, versionLabel);
                HBox.setHgrow(details, Priority.ALWAYS);

                // Status Indicator (Subtle ring)
                Circle statusDot = new Circle(4);
                statusDot.setStrokeWidth(2);
                statusDot.setStroke(Color.web("#1e1e1e")); // Inner separation

                statusDot.fillProperty().bind(Bindings.when(item.runningProperty())
                        .then(Color.web("#4caf50"))
                        .otherwise(Color.web("#f44336")));

                cellRoot.getChildren().addAll(iconContainer, details, statusDot);

                Icons.animateHover(cellRoot);
                setGraphic(cellRoot);

                // Context Menu
                ContextMenu menu = new ContextMenu();
                MenuItem deleteItem = new MenuItem("Delete Server");
                deleteItem.setGraphic(Icons.getDelete());
                deleteItem.setOnAction(e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete " + item.getName() + "?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(type -> {
                        if (type == ButtonType.YES) {
                            instanceManager.deleteInstance(item);
                            listView.getSelectionModel().clearSelection();
                        }
                    });
                });
                menu.getItems().add(deleteItem);
                setContextMenu(menu);
            }
        }
    }
}
