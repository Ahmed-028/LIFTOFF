package com.liftoff.app;

import com.liftoff.audio.Audio;
import com.liftoff.audio.Sfx;
import com.liftoff.audio.Track;
import com.liftoff.session.GameSession;
import com.liftoff.session.Level;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToggleButton;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.util.Duration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/** Switches between screens and hands each controller the shared game session. */
public final class SceneNavigator {

    private static final String FXML_FOLDER = "/com/liftoff/ui/";

    private final Scene scene;
    private final GameSession session;
    private final ResourceBundle messages;
    private final Consumer<String> openDocument;
    private ScreenController current;
    private FadeTransition entrance;
    private boolean reducedMotion;
    private List<Level> pendingRoute = List.of();
    private boolean titleLaunch;
    private Cutscene cutscene = Cutscene.INTRO;
    private java.util.function.BiFunction<Screen, Parent, Parent> decorator = (screen, root) -> root;
    private Runnable afterCutscene;

    public SceneNavigator(Scene scene, GameSession session, ResourceBundle messages) {
        this(scene, session, messages, url -> {});
    }

    public SceneNavigator(Scene scene, GameSession session, ResourceBundle messages, Consumer<String> openDocument) {
        this.scene = scene;
        this.session = session;
        this.messages = messages;
        this.openDocument = openDocument;
        // Every button makes a small sound: a pick or unpick for choices, a snap for ship parts,
        // a brighter "go" for the orange main action and a click for the rest.
        scene.addEventFilter(ActionEvent.ACTION, event -> {
            if (event.getTarget() instanceof ButtonBase button) {
                Audio.get().play(soundFor(button));
            }
        });
    }

    private static Sfx soundFor(ButtonBase button) {
        if (button instanceof CheckBox box) {
            return box.isSelected() ? Sfx.PICK : Sfx.UNPICK;
        }
        if (button instanceof ToggleButton) {
            return Sfx.CLICK;
        }
        if (button.getStyleClass().contains("part-card")) {
            return Sfx.SNAP;
        }
        return button.getStyleClass().contains("primary") ? Sfx.GO : Sfx.CLICK;
    }

    /** The music that suits a screen: calm in the hangar, bouncy in the games, a fanfare at the end. */
    private Track musicFor(Screen screen) {
        return switch (screen) {
            case STAR_MATCH, ASTEROID_DASH -> Track.ARCADE;
            case CUTSCENE -> cutscene == Cutscene.ENDING ? Track.FINALE : Track.HANGAR;
            default -> Track.HANGAR;
        };
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
        if (entrance != null) {
            entrance.stop();
        }
        Parent shown = decorator.apply(screen, root);
        scene.setRoot(shown);
        Audio.get().music(musicFor(screen));
        if (!reducedMotion) {
            entrance = new FadeTransition(Duration.millis(180), shown);
            entrance.setFromValue(.72);
            entrance.setToValue(1);
            entrance.play();
        }
    }

    /** Lets something wrap each screen as it opens (Nova's tutorial uses this to add its bubble). */
    public void setScreenDecorator(java.util.function.BiFunction<Screen, Parent, Parent> decorator) {
        this.decorator = decorator;
    }

    public boolean reducedMotion() {
        return reducedMotion;
    }

    public void setReducedMotion(boolean reducedMotion) {
        this.reducedMotion = reducedMotion;
        if (reducedMotion && entrance != null) {
            entrance.stop();
            scene.getRoot().setOpacity(1);
        }
    }

    /**
     * Shows the screen of the current mission step. A step in a level that is not open yet (the
     * mini-game before it is not played) sends the ship to the road map instead.
     */
    public void showMissionStep() {
        var level = Level.of(session.step());
        var levels = session.levels();
        if (!levels.isOpen(level)) {
            finishLevel(levels.position());
            return;
        }
        levels.moveTo(level);
        show(Screen.valueOf(session.step().name()));
    }

    /** Shows the road map; the ship flies along the route first (unless calm mode is on). */
    public void showRoadMap(List<Level> route) {
        pendingRoute = List.copyOf(route);
        show(Screen.ROAD_MAP);
    }

    /** The route the road map should fly once, then forgets it. */
    public List<Level> takeRoute() {
        var route = pendingRoute;
        pendingRoute = List.of();
        return route;
    }

    /** After the last step of a level: back to the road map, flying on to the next level to play. */
    public void finishLevel(Level finished) {
        showRoadMap(session.levels().routeAfter(finished));
    }

    /** Opens a level straight away, with no travel: a mini-game screen or the level's mission step. */
    public void enterLevel(Level level) {
        var levels = session.levels();
        levels.moveTo(level);
        switch (level) {
            case STAR_MATCH -> show(Screen.STAR_MATCH);
            case ASTEROID_DASH -> show(Screen.ASTEROID_DASH);
            default -> {
                session.goTo(levels.entryStep(level));
                showMissionStep();
            }
        }
    }

    /** Plays a cutscene, then runs {@code after} (also when the player skips it). */
    public void playCutscene(Cutscene kind, Runnable after) {
        cutscene = kind;
        afterCutscene = after;
        show(Screen.CUTSCENE);
    }

    /** The opening cutscene, ending on the title screen with its rocket launching. */
    public void playIntro() {
        playCutscene(Cutscene.INTRO, this::showTitleWithLaunch);
    }

    /** Which cutscene the cutscene screen should play. */
    public Cutscene cutscene() {
        return cutscene;
    }

    /** Leaves the cutscene for whatever comes after it. */
    public void finishCutscene() {
        var after = afterCutscene;
        afterCutscene = null;
        (after != null ? after : (Runnable) this::showTitleWithLaunch).run();
    }

    /** Shows the title screen with its rocket launching, as the end of the intro cutscene. */
    public void showTitleWithLaunch() {
        titleLaunch = true;
        show(Screen.TITLE);
    }

    /** Whether the title screen should launch its rocket now; true once after the intro. */
    public boolean takeTitleLaunch() {
        boolean launch = titleLaunch;
        titleLaunch = false;
        return launch;
    }

    public void openSource(String url) {
        String scheme = URI.create(url).getScheme();
        if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
            openDocument.accept(url);
        }
    }
}
