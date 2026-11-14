package com.liftoff.app;

import com.liftoff.data.CatalogException;
import com.liftoff.data.CatalogLoader;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionLog;
import javafx.application.Application;
import javafx.application.Platform;
import com.liftoff.render.RocketArt;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
        com.liftoff.audio.Audio.get().preload();
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
        var navigator = new SceneNavigator(scene, session, messages, getHostServices()::showDocument);
        navigator.setScreenDecorator(new com.liftoff.ui.NovaGuide(navigator, session, messages)::decorate);
        navigator.playIntro();

        stage.setTitle(messages.getString("app.title"));
        // Ready-made PNG files (made from icon(256) by the snapshot tool); Windows did not show icons drawn at start-up.
        for (int size : new int[] { 16, 24, 32, 48, 64, 128, 256 }) {
            stage.getIcons().add(new Image(LiftoffApp.class.getResourceAsStream("/com/liftoff/art/icon-" + size + ".png")));
        }
        stage.setMinWidth(MIN_WIDTH);
        stage.setScene(scene);
        stage.show();
        stage.setMinHeight(MIN_CONTENT_HEIGHT + stage.getHeight() - scene.getHeight());
    }

    /** Draws the icon art: the title rocket, flame lit, in front of the hangar window. The art/icon-*.png files are made from it. */
    public static Image icon(int size) {
        // The piece of the hangar window behind the title rocket: Earth on the left, stars on the right.
        var hangar = new Image(LiftoffApp.class.getResource("/com/liftoff/models/hangar.png").toExternalForm());
        double side = hangar.getHeight() * .444;
        var backdrop = new ImageView(hangar);
        backdrop.setViewport(new Rectangle2D((hangar.getWidth() - side) / 2, hangar.getHeight() * .36, side, side));
        backdrop.setFitWidth(size);
        backdrop.setFitHeight(size);
        backdrop.setSmooth(true);
        var corners = new Rectangle(size, size);
        corners.setArcWidth(size * .36);
        corners.setArcHeight(size * .36);
        backdrop.setClip(corners);
        var art = new StackPane(backdrop, RocketArt.create(size * .92));
        art.resize(size, size);
        art.layout();
        var transparent = new SnapshotParameters();
        transparent.setFill(Color.TRANSPARENT);
        return art.snapshot(transparent, null);
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
