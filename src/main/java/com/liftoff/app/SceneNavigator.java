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


    public void showRoadMap(List<Level> route) {
        pendingRoute = List.copyOf(route);
        show(Screen.ROAD_MAP);
    }


    public List<Level> takeRoute() {
        var route = pendingRoute;
        pendingRoute = List.of();
        return route;
    }


    public void finishLevel(Level finished) {
        showRoadMap(session.levels().routeAfter(finished));
    }


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


    public void playCutscene(Cutscene kind, Runnable after) {
        cutscene = kind;
        afterCutscene = after;
        show(Screen.CUTSCENE);
    }


    public void playIntro() {
        playCutscene(Cutscene.INTRO, this::showTitleWithLaunch);
    }


    public Cutscene cutscene() {
        return cutscene;
    }


    public void finishCutscene() {
        var after = afterCutscene;
        afterCutscene = null;
        (after != null ? after : (Runnable) this::showTitleWithLaunch).run();
    }


    public void showTitleWithLaunch() {
        titleLaunch = true;
        show(Screen.TITLE);
    }


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
