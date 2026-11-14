package com.liftoff.ui;

import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.audio.Audio;
import com.liftoff.audio.Sfx;
import com.liftoff.data.MissionEntry;
import com.liftoff.render3d.NovaPortrait;
import com.liftoff.session.GameSession;
import com.liftoff.session.Level;
import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * Nova's tutorial for the first mission. On each screen Nova says what to do next in a speech
 * bubble, and a glowing ring (with the rest of the screen dimmed a little) shows where to look.
 * The tutorial never blocks the screen: everything stays clickable, Next moves on, and
 * "Skip tutorial" turns it off. It only runs while the first mission of the ladder is chosen.
 */
public final class NovaGuide {

    /**
     * One thing Nova says.
     *
     * @param target CSS selector of the control to ring, or null for none
     * @param ready  when the step may show; steps wait until it is true
     */
    record Step(String id, String target, Predicate<GameSession> ready) {
        Step(String id, String target) {
            this(id, target, session -> true);
        }
    }

    private static final Map<Screen, List<Step>> SCRIPT = new EnumMap<>(Screen.class);

    static {
        SCRIPT.put(Screen.ROAD_MAP, List.of(
                new Step("hello", null),
                new Step("map", "#mapHost"),
                new Step("play", "#play-level"),
                new Step("travel", "#play-level", session -> session.levels().position() != Level.BUILD)));
        SCRIPT.put(Screen.BRIEFING, List.of(
                new Step("briefing", ".light-card"),
                new Step("briefingNext", ".action-bar .primary")));
        SCRIPT.put(Screen.SHIP, List.of(
                new Step("tabs", "#tabsHost"),
                new Step("parts", "#partsHost"),
                new Step("ship3d", "#hangarHost"),
                new Step("gauges", "#gaugesHost"),
                new Step("shipDone", "#supplies", session -> session.build().isComplete())));
        SCRIPT.put(Screen.STAR_MATCH, List.of(
                new Step("match", "#boardHost"),
                new Step("matchSkip", "#skip-game")));
        SCRIPT.put(Screen.SUPPLIES, List.of(
                new Step("rocket", "#rocket"),
                new Step("tools", "#supplyTabs"),
                new Step("readings", "#readout"),
                new Step("toReview", "#review")));
        SCRIPT.put(Screen.REVIEW, List.of(
                new Step("review", "#confirm")));
        SCRIPT.put(Screen.ASTEROID_DASH, List.of(
                new Step("dash", "#trackHost")));
        SCRIPT.put(Screen.LAUNCH, List.of(
                new Step("launch", "#check")));
        SCRIPT.put(Screen.FLY, List.of(
                new Step("fly", "#flight-views")));
        SCRIPT.put(Screen.REPORT, List.of(
                new Step("report", ".report-card"),
                new Step("goodbye", null)));
    }

    private final SceneNavigator navigator;
    private final GameSession session;
    private final UiText text;
    private final Map<Screen, Integer> progress = new EnumMap<>(Screen.class);
    private boolean enabled = true;

    public NovaGuide(SceneNavigator navigator, GameSession session, ResourceBundle messages) {
        this.navigator = navigator;
        this.session = session;
        this.text = new UiText(messages);
    }

    /** Wraps a screen with Nova's bubble when the tutorial has something to say there. */
    public Parent decorate(Screen screen, Parent root) {
        var steps = SCRIPT.get(screen);
        if (!enabled || steps == null || !firstMission() || progress.getOrDefault(screen, 0) >= steps.size()) {
            return root;
        }
        var layer = new Layer(screen, steps);
        var shell = new StackPane(root, layer);
        shell.getStyleClass().add("guide-shell");
        return shell;
    }

    private boolean firstMission() {
        var objective = session.design().objective();
        if (objective == null) {
            return false;
        }
        return session.catalog().missions().stream().filter(MissionEntry::ladder).findFirst()
                .map(first -> first.id().equals(objective.id())).orElse(false);
    }

    /** The see-through layer over a screen: the dimming, the ring and the bubble. */
    private final class Layer extends Pane {
        private static final double BUBBLE_WIDTH = 470;
        private final Screen screen;
        private final List<Step> steps;
        private final Rectangle ring = new Rectangle();
        private final Region dim = new Region();
        private Shape shade;
        private final VBox bubble = new VBox(10);
        private final Label words = new Label();
        private final Button next = new Button();
        private final NovaPortrait nova = new NovaPortrait(118, 132);
        private final AnimationTimer follow;
        private int shown = -1;

        Layer(Screen screen, List<Step> steps) {
            this.screen = screen;
            this.steps = steps;
            setPickOnBounds(false);
            getStyleClass().add("guide-layer");
            ring.getStyleClass().add("guide-ring");
            ring.setArcWidth(26);
            ring.setArcHeight(26);
            ring.setMouseTransparent(true);
            dim.setMouseTransparent(true);

            var name = new Label(text.get("guide.name"));
            name.getStyleClass().add("guide-name");
            words.getStyleClass().add("guide-words");
            words.setWrapText(true);
            words.setMinHeight(Region.USE_PREF_SIZE);
            next.setId("guide-next");
            next.getStyleClass().add("guide-next");
            next.setOnAction(event -> advance());
            var skip = new Hyperlink(text.get("guide.skip"));
            skip.setId("guide-skip");
            skip.getStyleClass().add("guide-skip");
            skip.setOnAction(event -> {
                enabled = false;
                hideAll();
            });
            var spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            var buttons = new HBox(10, skip, spacer, next);
            buttons.setAlignment(Pos.CENTER_LEFT);
            var speech = new VBox(6, name, words, buttons);
            HBox.setHgrow(speech, Priority.ALWAYS);
            var row = new HBox(12, nova, speech);
            row.setAlignment(Pos.CENTER_LEFT);
            bubble.getChildren().add(row);
            bubble.getStyleClass().add("guide-bubble");
            bubble.setPrefWidth(BUBBLE_WIDTH);
            bubble.setMaxWidth(BUBBLE_WIDTH);
            bubble.setAccessibleRoleDescription(text.get("guide.name"));
            getChildren().addAll(ring, bubble);
            hideAll();

            follow = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    place();
                }
            };
            sceneProperty().addListener((property, before, scene) -> {
                if (scene == null) {
                    follow.stop();
                    // Leaving the screen counts as having heard what Nova was saying.
                    if (shown >= 0 && shown == index()) {
                        progress.put(screen, index() + 1);
                    }
                } else {
                    follow.start();
                }
            });
        }

        private int index() {
            return progress.getOrDefault(screen, 0);
        }

        private void advance() {
            progress.put(screen, index() + 1);
            if (index() >= steps.size()) {
                hideAll();
            }
        }

        private void hideAll() {
            bubble.setVisible(false);
            ring.setVisible(false);
            if (shade != null) {
                shade.setVisible(false);
            }
        }

        /** Every frame: show the current step if it is ready, and keep the ring on its target. */
        private void place() {
            if (!enabled || index() >= steps.size()) {
                hideAll();
                return;
            }
            var step = steps.get(index());
            if (!step.ready().test(session)) {
                hideAll();
                return;
            }
            if (shown != index()) {
                shown = index();
                words.setText(text.get("guide." + step.id()));
                words.setAccessibleText(words.getText());
                next.setText(text.get(index() == steps.size() - 1 ? "guide.gotIt" : "guide.next"));
                nova.talk();
                Audio.get().play(Sfx.POP);
            }
            bubble.setVisible(true);
            Bounds target = targetBounds(step);
            bubble.applyCss();
            bubble.autosize();
            double bubbleHeight = bubble.getHeight();
            double width = getWidth();
            double height = getHeight();
            double x = 24;
            double y = height - bubbleHeight - 110;
            if (target != null) {
                ring.setVisible(true);
                ring.setX(target.getMinX() - 8);
                ring.setY(target.getMinY() - 8);
                ring.setWidth(target.getWidth() + 16);
                ring.setHeight(target.getHeight() + 16);
                drawShade(target, width, height);
                // Put the bubble in the corner farthest from what Nova is pointing at.
                boolean targetLeft = target.getCenterX() < width / 2;
                boolean targetLow = target.getCenterY() > height / 2;
                x = targetLeft ? width - BUBBLE_WIDTH - 24 : 24;
                y = targetLow ? 104 : height - bubbleHeight - 104;
                if (target.getWidth() > width * .55) {
                    x = (width - BUBBLE_WIDTH) / 2;
                }
            } else {
                ring.setVisible(false);
                if (shade != null) {
                    shade.setVisible(false);
                }
                x = (width - BUBBLE_WIDTH) / 2;
                y = (height - bubbleHeight) / 2;
            }
            bubble.relocate(Math.max(12, x), Math.max(12, Math.min(y, height - bubbleHeight - 12)));
        }

        private Bounds targetBounds(Step step) {
            if (step.target() == null || getScene() == null) {
                return null;
            }
            Node node = getScene().lookup(step.target());
            if (node == null || !node.isVisible() || node.getScene() == null) {
                return null;
            }
            Bounds inScene = node.localToScene(node.getBoundsInLocal());
            return sceneToLocal(inScene);
        }

        private void drawShade(Bounds target, double width, double height) {
            if (shade != null) {
                getChildren().remove(shade);
            }
            var hole = new Rectangle(target.getMinX() - 8, target.getMinY() - 8, target.getWidth() + 16, target.getHeight() + 16);
            hole.setArcWidth(26);
            hole.setArcHeight(26);
            shade = Shape.subtract(new Rectangle(width, height), hole);
            shade.setFill(Color.web("#050E26", .38));
            shade.setMouseTransparent(true);
            getChildren().add(0, shade);
        }
    }
}
