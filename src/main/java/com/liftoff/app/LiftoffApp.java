package com.liftoff.app;

import com.liftoff.data.CatalogException;
import com.liftoff.data.CatalogLoader;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionLog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ResourceBundle;

/** The JavaFX application: loads the catalogs, creates the game session and opens the first screen. */
public final class LiftoffApp extends Application {

    public static final String MESSAGES_BUNDLE = "com.liftoff.i18n.messages";
    public static final String STYLESHEET = "/com/liftoff/css/liftoff.css";

    private static final double START_WIDTH = 1280;
    private static final double START_HEIGHT = 800;
    private static final double MIN_WIDTH = 1024;
    private static final double MIN_CONTENT_HEIGHT = 660;

    @Override
    public void start(Stage stage) {
        loadFonts();
        ResourceBundle messages = ResourceBundle.getBundle(MESSAGES_BUNDLE);

        GameSession session;
        try {
            var catalog = CatalogLoader.loadFromClasspath();
            session = new GameSession(catalog, catalog::shipPart, new MissionLog());
        } catch (CatalogException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.setHeaderText(messages.getString("error.catalog"));
            alert.showAndWait();
            Platform.exit();
            return;
        }

        Scene scene = new Scene(new StackPane(), START_WIDTH, START_HEIGHT);
        scene.getStylesheets().add(LiftoffApp.class.getResource(STYLESHEET).toExternalForm());
        new SceneNavigator(scene, session, messages, getHostServices()::showDocument).show(Screen.TITLE);

        stage.setTitle(messages.getString("app.title"));
        stage.setMinWidth(MIN_WIDTH);
        stage.setScene(scene);
        stage.show();
        stage.setMinHeight(MIN_CONTENT_HEIGHT + stage.getHeight() - scene.getHeight());
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void loadFonts() {
        for (String name : new String[] { "Barlow-Regular", "Barlow-Bold", "BarlowCondensed-BoldItalic" }) {
            var font = LiftoffApp.class.getResource("/com/liftoff/fonts/" + name + ".ttf");
            Font.loadFont(font.toExternalForm(), 14);
        }
    }
}
