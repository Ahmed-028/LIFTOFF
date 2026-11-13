package com.liftoff.app;

import com.liftoff.data.CatalogException;
import com.liftoff.data.CatalogLoader;
import com.liftoff.model.ShipPart;
import com.liftoff.render.Images;
import com.liftoff.render3d.HangarShipView;
import com.liftoff.render3d.ModelLibrary;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionLog;
import com.liftoff.util.Background;
import com.liftoff.util.ErrorLog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/** The JavaFX application: loads the catalogs, creates the game session and opens the first screen. */
public final class LiftoffApp extends Application {

    public static final String MESSAGES_BUNDLE = "com.liftoff.i18n.messages";
    public static final String STYLESHEET = "/com/liftoff/css/liftoff.css";

    private static final double START_WIDTH = 1280;
    private static final double START_HEIGHT = 800;
    private static final double MIN_WIDTH = 1024;
    private static final double MIN_CONTENT_HEIGHT = 660;

    /** Writes the mission log, one save after another. */
    private static final ExecutorService LOG_WRITER = Background.serial("liftoff-log-writer");

    /**
     * Shows the title at once. The catalog, the mission log, the 3D models and the hangar picture
     * load in the background; the title's start button waits for them.
     */
    @Override
    public void start(Stage stage) {
        loadFonts();
        ResourceBundle messages = ResourceBundle.getBundle(MESSAGES_BUNDLE);
        Images.preload(HangarShipView.BACKDROP);

        Scene scene = new Scene(new StackPane(), START_WIDTH, START_HEIGHT);
        scene.getStylesheets().add(LiftoffApp.class.getResource(STYLESHEET).toExternalForm());
        SceneNavigator navigator = new SceneNavigator(scene, messages, getHostServices()::showDocument,
                ErrorLog.inUserHome());
        installCrashHandler(navigator);
        navigator.show(Screen.TITLE);

        stage.setTitle(messages.getString("app.title"));
        stage.setMinWidth(MIN_WIDTH);
        stage.setScene(scene);
        stage.show();
        stage.setMinHeight(MIN_CONTENT_HEIGHT + stage.getHeight() - scene.getHeight());

        CompletableFuture.supplyAsync(LiftoffApp::loadGame, Background.POOL).whenComplete((session, problem) ->
                Platform.runLater(() -> {
                    if (problem == null) {
                        navigator.setSession(session);
                    } else if (problem.getCause() instanceof CatalogException broken) {
                        showCatalogError(messages, broken);
                    } else {
                        navigator.showError(problem.getCause() == null ? problem : problem.getCause());
                    }
                }));
    }

    /** Off the FX thread: the catalog, the mission log, and every 3D model in catalog order. */
    private static GameSession loadGame() {
        var catalog = CatalogLoader.loadFromClasspath();
        ModelLibrary.preload(catalog.shipParts().stream().map(ShipPart::modelId).distinct().toList());
        return new GameSession(catalog, catalog::shipPart, new MissionLog(MissionLog.defaultPath(), LOG_WRITER));
    }

    private static void showCatalogError(ResourceBundle messages, CatalogException problem) {
        Alert alert = new Alert(Alert.AlertType.ERROR, problem.getMessage());
        alert.setHeaderText(messages.getString("error.catalog"));
        alert.showAndWait();
        Platform.exit();
    }

    /**
     * Sends every exception nothing else caught, on any thread, to the navigator's error screen:
     * the player sees "Something went wrong, go back" and the cause is logged. JavaFX hands errors
     * from event handlers and animations to this handler too.
     */
    public static void installCrashHandler(SceneNavigator navigator) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            if (Platform.isFxApplicationThread()) {
                navigator.showError(error);
            } else {
                Platform.runLater(() -> navigator.showError(error));
            }
        });
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
