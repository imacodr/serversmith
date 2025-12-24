package dev.perillo.serversmith.ui;

import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

public class Icons {

    // Material Design SVG Paths
    private static final String PATH_PLAY = "M8 5v14l11-7z";
    private static final String PATH_STOP = "M6 6h12v12H6z";
    private static final String PATH_SETTINGS = "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z";
    private static final String PATH_CONSOLE = "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-1.22 9.27l-4.47 4.47-1.41-1.41 3.05-3.05-3.04-3.05 1.41-1.41 4.47 4.47c.39.39.39 1.02 0 1.41zM8 12.5h8v2H8v-2z"; // Simplified/Modified
    private static final String PATH_DELETE = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
    private static final String PATH_FOLDER = "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z";
    private static final String PATH_FILE = "M13 11h-2v3H8v2h3v3h2v-3h3v-2h-3v-3zm1-9H6c-1.1 0-2 .9-2 2v16c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z";

    // Custom/Approximations for Server Types
    private static final String PATH_CUBE = "M12 2L2 7l10 5 10-5-10-5zm0 9l2.5-1.25L12 8.5 9.5 9.75 12 11zm0 2.5l-5-2.5-5 2.5L12 22l10-8.5-5-2.5-5 2.5z"; // Very
                                                                                                                                                          // rough
                                                                                                                                                          // cube
    private static final String PATH_SCROLL = "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z";
    private static final String PATH_ANVIL = "M21,18V15C19,15 17.5,14 16.5,13L16.2,12.7C15,11.5 14,10 14,8V4H10V8C10,10 9,11.5 7.8,12.7L7.5,13C6.5,14 5,15 3,15V18H9V20H15V18H21Z";

    public static SVGPath create(String pathData, double scale) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.setScaleX(scale);
        path.setScaleY(scale);
        path.getStyleClass().add("icon");
        return path;
    }

    public static SVGPath getPlay() {
        return create(PATH_PLAY, 1.2);
    }

    public static SVGPath getStop() {
        return create(PATH_STOP, 1.2);
    }

    public static SVGPath getSettings() {
        return create(PATH_SETTINGS, 1.0);
    }

    public static SVGPath getConsole() {
        return create(PATH_CONSOLE, 1.0);
    }

    public static SVGPath getDelete() {
        return create(PATH_DELETE, 1.0);
    }

    public static SVGPath getFolder() {
        return create(PATH_FOLDER, 1.0);
    }

    public static SVGPath getFile() {
        return create(PATH_FILE, 1.0);
    }

    public static SVGPath getVanilla() {
        return create(PATH_CUBE, 2.0);
    } // Bigger for wizard

    public static SVGPath getSmallVanilla() {
        return create(PATH_CUBE, 1.0);
    }

    public static SVGPath getPaper() {
        return create(PATH_SCROLL, 2.0);
    }

    public static SVGPath getSmallPaper() {
        return create(PATH_SCROLL, 1.0);
    }

    public static SVGPath getNeoForge() {
        return create(PATH_ANVIL, 2.0);
    }

    public static SVGPath getSmallNeoForge() {
        return create(PATH_ANVIL, 1.0);
    }

    public static void animateHover(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
        node.setOnMouseEntered(e -> {
            st.setToX(1.05); // More subtle
            st.setToY(1.05);
            st.playFromStart();
        });
        node.setOnMouseExited(e -> {
            st.setToX(1.0);
            st.setToY(1.0);
            st.playFromStart();
        });
    }

    public static void animateClick(Node node) {
        // Pulse
        ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.95);
        st.setToY(0.95);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }
}
