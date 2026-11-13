package com.liftoff.app;

import com.liftoff.render.Icons;
import com.liftoff.session.GameSession;
import com.liftoff.util.ErrorLog;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Switches between screens and hands each controller the shared game session. Also the last line
 * of defence: a screen that fails to open, or any error handed to {@link #showError}, becomes a
 * friendly "something went wrong" screen with a way back, and the cause goes to the error log.
 */
public final class SceneNavigator {

    private static final String FXML_FOLDER = "/com/liftoff/ui/";

    private final Scene scene;
    private final List<Runnable> waitingForSession = new ArrayList<>();
    private GameSession session;
    private final ResourceBundle messages;
    private final Consumer<String> openDocument;
    private final ErrorLog errorLog;
    private ScreenController current;

    public SceneNavigator(Scene scene, GameSession session, ResourceBundle messages) {
        this(scene, session, messages, url -> {});
    }

    public SceneNavigator(Scene scene, GameSession session, ResourceBundle messages, Consumer<String> openDocument) {
        this(scene, session, messages, openDocument, new ErrorLog(null));
    }

    public SceneNavigator(Scene scene, GameSession session, ResourceBundle messages, Consumer<String> openDocument,
                          ErrorLog errorLog) {
        this.scene = scene;
        this.session = session;
        this.messages = messages;
        this.openDocument = openDocument;
        this.errorLog = errorLog;
    }

    /** A navigator whose game data is still loading: only the title can be shown until {@link #setSession}. */
    public SceneNavigator(Scene scene, ResourceBundle messages, Consumer<String> openDocument, ErrorLog errorLog) {
        this(scene, null, messages, openDocument, errorLog);
    }

    /** Hands over the game once its data has loaded, and tells whoever was waiting. FX thread only. */
    public void setSession(GameSession loaded) {
        session = loaded;
        List<Runnable> waiting = List.copyOf(waitingForSession);
        waitingForSession.clear();
        waiting.forEach(Runnable::run);
    }

    public boolean isReady() {
        return session != null;
    }

    /** Runs the action now if the game data is loaded, otherwise as soon as it is. */
    public void whenReady(Runnable action) {
        if (isReady()) {
            action.run();
        } else {
            waitingForSession.add(action);
        }
    }

    /** Opens the screen. If it cannot be opened, the error screen is shown instead; this never throws. */
    public void show(Screen screen) {
        try {
            open(screen);
        } catch (RuntimeException | LinkageError problem) {
            showError(problem);
        }
    }

    public void showMissionStep() {
        show(Screen.valueOf(session.step().name()));
    }

    /**
     * Brings the screen up to date with the session now instead of on the next pulse. For tools
     * that drive the game faster than a player can click.
     */
    public void settle() {
        if (current != null) {
            current.settle();
        }
    }

    /** The controller of the screen now showing; null on the error screen. */
    public ScreenController current() {
        return current;
    }

    public void openSource(String url) {
        String scheme = URI.create(url).getScheme();
        if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
            openDocument.accept(url);
        }
    }

    /**
     * Logs the cause and replaces whatever is showing with a calm message and a "Go back" button.
     * Built in code, not from a layout file, so that it cannot fail the way a screen can.
     */
    public void showError(Throwable cause) {
        errorLog.record(cause);
        closeCurrent();

        Label title = new Label(messages.getString("error.title"));
        title.setId("error-title");
        title.getStyleClass().add("screen-heading");
        title.setGraphic(Icons.warning(34));
        title.setGraphicTextGap(14);
        title.setWrapText(true);

        Label body = new Label(messages.getString("error.body"));
        body.setId("error-body");
        body.getStyleClass().add("body-text");
        body.setWrapText(true);
        body.setMaxWidth(520);

        Button back = new Button(messages.getString("error.back"));
        back.setId("error-back");
        back.getStyleClass().addAll("primary", "big");
        back.setDefaultButton(true);
        back.setOnAction(event -> goBack());

        VBox panel = new VBox(18, title, body, back);
        panel.getStyleClass().add("panel");
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setMaxSize(600, VBox.USE_PREF_SIZE);

        StackPane root = new StackPane(panel);
        root.setId("error-screen");
        root.getStyleClass().add("screen");
        scene.setRoot(root);
        back.requestFocus();
    }

    /** Back to the mission list; if even that cannot open, to the title; failing both, the error screen stays. */
    private void goBack() {
        for (Screen safe : isReady() ? new Screen[] {Screen.MISSION_SELECT, Screen.TITLE} : new Screen[] {Screen.TITLE}) {
            try {
                open(safe);
                return;
            } catch (RuntimeException | LinkageError problem) {
                errorLog.record(problem);
            }
        }
    }

    private void open(Screen screen) {
        if (session == null && screen != Screen.TITLE) {
            throw new IllegalStateException("The game data has not loaded yet: cannot open " + screen);
        }
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
        ScreenController next = loader.getController();
        closeCurrent();
        try {
            next.attach(this, session, messages);
        } catch (RuntimeException | LinkageError problem) {
            closeQuietly(next);
            throw problem;
        }
        current = next;
        scene.setRoot(root);
    }

    private void closeCurrent() {
        ScreenController closing = current;
        current = null;
        closeQuietly(closing);
    }

    /** A screen that is half built, or already broken, must not stop the next one from opening. */
    private void closeQuietly(ScreenController controller) {
        if (controller == null) {
            return;
        }
        try {
            controller.dispose();
        } catch (RuntimeException problem) {
            errorLog.record(problem);
        }
    }
}
