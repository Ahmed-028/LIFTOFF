package com.liftoff.app;

import com.liftoff.session.GameSession;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;

/** Switches between screens and hands each controller the shared game session. */
public final class SceneNavigator {

    private static final String FXML_FOLDER = "/com/liftoff/ui/";

    private final Scene scene;
    private final GameSession session;
    private final ResourceBundle messages;
    private ScreenController current;

    public SceneNavigator(Scene scene, GameSession session, ResourceBundle messages) {
        this.scene = scene;
        this.session = session;
        this.messages = messages;
    }

    public void show(Screen screen) {
        URL layout = SceneNavigator.class.getResource(FXML_FOLDER + screen.fxmlFile());
        if (layout == null) {
            throw new IllegalStateException("Screen layout not found: " + screen.fxmlFile());
        }
        FXMLLoader loader = new FXMLLoader(layout, messages);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load screen " + screen, e);
        }
        if (current != null) {
            current.dispose();
        }
        current = loader.getController();
        current.attach(this, session, messages);
        scene.setRoot(root);
    }
}
