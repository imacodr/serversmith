package dev.perillo.serversmith.ui;

import dev.perillo.serversmith.model.ServerInstance;
import dev.perillo.serversmith.service.InstanceManager;
import dev.perillo.serversmith.service.ProcessService;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

public class MainController {
    private final BorderPane root = new BorderPane();
    private final InstanceManager instanceManager = new InstanceManager();
    private final ProcessService processService = new ProcessService();

    private final SidebarController sidebar;
    private final EditorController editor;

    public MainController() {
        sidebar = new SidebarController(instanceManager, this::onServerSelected, this::onCreateRequested);
        editor = new EditorController(processService);

        SplitPane split = new SplitPane();
        split.getItems().addAll(sidebar.getView(), editor.getView());
        split.setDividerPositions(0.25);

        root.setCenter(split);
    }

    public Parent getView() {
        return root;
    }

    private void onServerSelected(ServerInstance instance) {
        editor.setServer(instance);
    }

    private void onCreateRequested() {
        new WizardDialog(instanceManager).showAndWait();
    }
}
