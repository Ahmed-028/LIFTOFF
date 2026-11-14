package com.liftoff.ui;

import com.liftoff.app.Screen;
import com.liftoff.render.Icons;
import com.liftoff.render.RocketArt;
import com.liftoff.render.Theme;
import com.liftoff.session.Level;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.List;
import java.util.SplittableRandom;

/**
 * The mission road map: five levels along a winding route. After a level is finished the ship
 * flies along the route to the next level to play; any open level can be picked straight away.
 */
public final class RoadMapController extends MissionScreen {

    private static final double WIDTH = 1180;
    private static final double HEIGHT = 430;
    private static final double PLANET = 44;
    private static final double SHIP_LIFT = 62;
    private static final double[][] STOPS = {{100, 292}, {345, 150}, {590, 286}, {835, 140}, {1080, 262}};

    @FXML private StackPane mapHost;
    @FXML private StackPane statusIcon;
    @FXML private Label statusTitle;
    @FXML private Label statusText;
    @FXML private CheckBox calm;
    @FXML private Button play;

    private final Pane map = new Pane();
    private final Group ship = new Group();
    private final Path doneTrail = new Path();
    private final EnumMap<Level, Button> nodes = new EnumMap<>(Level.class);
    private final EnumMap<Level, Label> states = new EnumMap<>(Level.class);
    private Animation travel;
    private Level arriving;

    @Override
    protected Level level() {
        return session.levels().position();
    }

    @Override
    protected boolean showsMapButton() {
        return false;
    }

    @Override
    protected boolean showsStepper() {
        return false;
    }

    @Override
    protected void setUp() {
        calm.setSelected(navigator.reducedMotion());
        calm.selectedProperty().addListener((property, previous, enabled) -> {
            navigator.setReducedMotion(enabled);
            if (enabled && travel != null) {
                travel.jumpTo("end");
            }
        });
        map.setPrefSize(WIDTH, HEIGHT);
        map.setMinSize(WIDTH, HEIGHT);
        map.setMaxSize(WIDTH, HEIGHT);
        drawSky();
        drawRoute();
        for (Level level : Level.values()) {
            map.getChildren().add(stop(level));
        }
        drawShip();
        var scaled = new Group(map);
        mapHost.getChildren().add(scaled);
        Runnable fit = () -> {
            double scale = Math.min(1.25, Math.min(mapHost.getWidth() / (WIDTH + 24), mapHost.getHeight() / (HEIGHT + 16)));
            map.setScaleX(Math.max(.4, scale));
            map.setScaleY(Math.max(.4, scale));
        };
        mapHost.widthProperty().addListener((property, previous, width) -> fit.run());
        mapHost.heightProperty().addListener((property, previous, height) -> fit.run());

        var route = navigator.takeRoute();
        Level start = route.isEmpty() ? session.levels().position() : Level.values()[route.get(0).ordinal() - 1];
        park(start);
        if (!route.isEmpty()) {
            fly(start, route);
        }
    }

    @Override
    protected void update() {
        var levels = session.levels();
        for (Level level : Level.values()) {
            var node = nodes.get(level);
            node.getStyleClass().removeAll("level-done", "level-here", "level-open", "level-locked");
            String state;
            if (!levels.isOpen(level)) {
                node.getStyleClass().add("level-locked");
                state = text.get("level.state.locked");
            } else if (levels.isDone(level)) {
                node.getStyleClass().add("level-done");
                state = text.get("level.state.done");
            } else {
                node.getStyleClass().add("level-open");
                state = text.get("level.state.open");
            }
            if (level == levels.position() && travel == null) {
                node.getStyleClass().add("level-here");
            }
            states.get(level).setText(state);
            states.get(level).getStyleClass().remove("level-done");
            if (levels.isOpen(level) && levels.isDone(level)) {
                states.get(level).getStyleClass().add("level-done");
            }
            String name = text.get("level.name." + level.name());
            node.setAccessibleText(text.get("level.accessible", level.number(), name, state));
            node.setTooltip(new Tooltip(levels.isOpen(level) ? text.get("level.about." + level.name())
                    : lockedReason(level)));
        }
        drawDoneTrail();
        if (travel != null) {
            return;
        }
        if (levels.allDone()) {
            statusIcon.getChildren().setAll(star(22, Theme.AMBER));
            statusTitle.setText(text.get("map.complete"));
            boolean ending = session.finishedFinalMission();
            statusText.setText(text.get(ending ? "map.endingHint" : "map.completeHint"));
            play.setText(text.get(ending ? "map.ending" : "map.another"));
        } else {
            Level next = levels.toPlay();
            statusIcon.getChildren().setAll(Icons.next(40));
            statusTitle.setText(text.get("map.next", next.number(), text.get("level.name." + next.name())));
            statusText.setText(text.get("level.about." + next.name()));
            play.setText(text.get("map.play", next.number()));
        }
    }

    @FXML
    private void onPlay() {
        if (travel != null) {
            travel.stop();
            travel = null;
        }
        if (session.levels().allDone() && session.finishedFinalMission()) {
            navigator.playCutscene(com.liftoff.app.Cutscene.ENDING, () -> navigator.show(Screen.TITLE));
        } else if (session.levels().allDone()) {
            navigator.show(Screen.MISSION_SELECT);
        } else {
            navigator.enterLevel(session.levels().toPlay());
        }
    }

    @Override
    public void dispose() {
        if (travel != null) {
            travel.stop();
        }
        super.dispose();
    }

    // ---- drawing ----

    private void drawSky() {
        var world = Theme.World.of(mission().destination());
        var random = new SplittableRandom(7);
        for (int i = 0; i < 90; i++) {
            var star = new Circle(random.nextDouble(WIDTH), random.nextDouble(HEIGHT), random.nextDouble(.8, 2.4));
            star.setFill(Theme.alpha(i % 7 == 0 ? world.accent : Theme.TEXT, random.nextDouble(.25, .8)));
            star.setMouseTransparent(true);
            map.getChildren().add(star);
        }
    }

    private void drawRoute() {
        var road = route(0);
        road.setStroke(Theme.alpha(Theme.NAVY_950, .55));
        road.setStrokeWidth(26);
        road.setStrokeLineCap(StrokeLineCap.ROUND);
        var dashes = route(0);
        dashes.setStroke(Theme.alpha(Theme.TEXT_SOFT, .7));
        dashes.setStrokeWidth(4);
        dashes.getStrokeDashArray().addAll(4.0, 14.0);
        dashes.setStrokeLineCap(StrokeLineCap.ROUND);
        doneTrail.setStroke(Theme.CYAN);
        doneTrail.setStrokeWidth(7);
        doneTrail.setStrokeLineCap(StrokeLineCap.ROUND);
        doneTrail.setEffect(new DropShadow(14, Theme.alpha(Theme.CYAN, .7)));
        for (Path path : new Path[] {road, dashes, doneTrail}) {
            path.setFill(null);
            path.setMouseTransparent(true);
        }
        map.getChildren().addAll(road, dashes, doneTrail);
    }

    /** The route through every stop, lifted by the given height above the planets. */
    private static Path route(double lift) {
        var path = new Path(new MoveTo(STOPS[0][0], STOPS[0][1] - lift));
        for (int i = 1; i < STOPS.length; i++) {
            path.getElements().add(curve(i, lift));
        }
        return path;
    }

    private static CubicCurveTo curve(int to, double lift) {
        double[] a = STOPS[to - 1];
        double[] b = STOPS[to];
        double half = (b[0] - a[0]) / 2;
        return new CubicCurveTo(a[0] + half, a[1] - lift, b[0] - half, b[1] - lift, b[0], b[1] - lift);
    }

    private void drawDoneTrail() {
        doneTrail.getElements().setAll(new MoveTo(STOPS[0][0], STOPS[0][1]));
        var levels = session.levels();
        Level shown = arriving != null ? arriving : levels.position();
        for (int i = 1; i < STOPS.length && i <= shown.ordinal(); i++) {
            if (levels.isDone(Level.values()[i - 1])) {
                doneTrail.getElements().add(curve(i, 0));
            }
        }
    }

    private Node stop(Level level) {
        double x = STOPS[level.ordinal()][0];
        double y = STOPS[level.ordinal()][1];
        var planet = new Circle(PLANET, planetFill(level));
        planet.setStroke(Theme.alpha(Theme.NAVY_950, .8));
        planet.setStrokeWidth(4);
        var number = new Label(Integer.toString(level.number()));
        number.getStyleClass().add("level-number");
        var face = new StackPane(planet, number);
        if (level.kind() == Level.Kind.MINI_GAME) {
            var badge = star(13, Theme.AMBER);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            face.getChildren().add(badge);
        }
        if (level == Level.LIFTOFF) {
            var ring = new Circle(PLANET + 9);
            ring.setFill(null);
            ring.setStroke(Theme.alpha(Theme.TEXT, .5));
            ring.setStrokeWidth(2);
            ring.getStrokeDashArray().addAll(6.0, 6.0);
            face.getChildren().add(0, ring);
        }
        var node = new Button(null, face);
        node.setId("level-" + level.name().toLowerCase().replace('_', '-'));
        node.getStyleClass().add("level-node");
        node.setOnAction(event -> choose(level));
        nodes.put(level, node);

        var eyebrow = new Label(text.get("level.eyebrow",
                level.number()));
        eyebrow.getStyleClass().add("level-eyebrow");
        var name = new Label(level == Level.LIFTOFF
                ? text.get("level.name.LIFTOFF.to", text.get("map.destination." + mission().destination().name()))
                : text.get("level.name." + level.name()));
        name.getStyleClass().add("level-name");
        var state = new Label();
        state.getStyleClass().add("level-state");
        states.put(level, state);
        var caption = new VBox(2, eyebrow, name, state);
        caption.setAlignment(Pos.TOP_CENTER);
        caption.getStyleClass().add("level-caption");
        caption.setMouseTransparent(true);

        var column = new VBox(6, node, caption);
        column.setAlignment(Pos.TOP_CENTER);
        column.setPrefWidth(200);
        column.setLayoutX(x - 100);
        column.setLayoutY(y - PLANET - 10);
        return column;
    }

    private Color[] planetColours(Level level) {
        var world = Theme.World.of(mission().destination());
        return switch (level) {
            case BUILD -> new Color[] {Theme.HULL_WHITE, Theme.SHELL_LINE_STRONG};
            case STAR_MATCH -> new Color[] {Theme.AMBER, Theme.ORANGE};
            case PACK -> new Color[] {Theme.CYAN_SOFT, Theme.NAVY_600};
            case ASTEROID_DASH -> new Color[] {Theme.GOOD, Theme.CARD_RING};
            case LIFTOFF -> new Color[] {world.accent, world.raised};
        };
    }

    private RadialGradient planetFill(Level level) {
        var colours = planetColours(level);
        return new RadialGradient(0, 0, .35, .3, .8, true, CycleMethod.NO_CYCLE,
                new Stop(0, colours[0]), new Stop(1, colours[1]));
    }

    private void drawShip() {
        var rocket = RocketArt.create(74);
        rocket.setRotate(90);
        ship.getChildren().add(new Group(rocket));
        ship.setMouseTransparent(true);
        ship.setEffect(new DropShadow(18, Theme.alpha(Theme.CYAN, .55)));
        map.getChildren().add(ship);
    }

    private void park(Level level) {
        ship.applyCss();
        var bounds = ship.getLayoutBounds();
        double[] at = STOPS[level.ordinal()];
        ship.setTranslateX(at[0] - (bounds.getMinX() + bounds.getWidth() / 2));
        ship.setTranslateY(at[1] - SHIP_LIFT - (bounds.getMinY() + bounds.getHeight() / 2));
        ship.setRotate(-90);
    }

    private void fly(Level from, List<Level> route) {
        var target = route.get(route.size() - 1);
        arriving = from;
        statusIcon.getChildren().setAll(RocketArt.create(40));
        statusTitle.setText(text.get("map.flying", target.number(), text.get("level.name." + target.name())));
        statusText.setText(text.get("map.flyingHint"));
        play.setText(text.get("map.play", target.number()));
        if (navigator.reducedMotion()) {
            arrive(target);
            return;
        }
        com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.WHOOSH);
        var legs = new SequentialTransition();
        var lift = new RotateTransition(Theme.MOTION_QUICK, ship);
        lift.setToAngle(0);
        legs.getChildren().add(lift);
        for (Level stop : route) {
            var leg = new Path(new MoveTo(STOPS[stop.ordinal() - 1][0], STOPS[stop.ordinal() - 1][1] - SHIP_LIFT),
                    curve(stop.ordinal(), SHIP_LIFT));
            var move = new PathTransition(Theme.MOTION_TRAVEL, leg, ship);
            move.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
            move.setInterpolator(Interpolator.EASE_BOTH);
            move.setOnFinished(event -> {
                arriving = stop;
                drawDoneTrail();
                com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.POP);
                pulse(nodes.get(stop));
            });
            legs.getChildren().add(move);
        }
        var upright = new RotateTransition(Duration.millis(300), ship);
        upright.setToAngle(-90);
        legs.getChildren().add(upright);
        legs.setOnFinished(event -> arrive(target));
        travel = legs;
        travel.play();
    }

    private void arrive(Level target) {
        com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.ARRIVE);
        travel = null;
        arriving = null;
        park(target);
        if (session.levels().isOpen(target)) {
            session.levels().moveTo(target);
        }
        update();
        statusTitle.setText(text.get("map.arrived", target.number(), text.get("level.name." + target.name())));
        play.requestFocus();
    }

    private static void pulse(Node node) {
        var grow = new ScaleTransition(Duration.millis(220), node);
        grow.setToX(1.18);
        grow.setToY(1.18);
        grow.setAutoReverse(true);
        grow.setCycleCount(2);
        grow.play();
    }

    private void choose(Level level) {
        if (travel != null) {
            return;
        }
        if (!session.levels().isOpen(level)) {
            statusIcon.getChildren().setAll(lock());
            statusTitle.setText(text.get("level.name." + level.name()));
            statusText.setText(lockedReason(level));
            return;
        }
        navigator.enterLevel(level);
    }

    private String lockedReason(Level level) {
        for (Level earlier : Level.values()) {
            if (!session.levels().isDone(earlier)) {
                return text.get("level.locked", earlier.number(), text.get("level.name." + earlier.name()));
            }
        }
        return text.get("level.state.locked");
    }

    static Polygon star(double radius, Color colour) {
        var star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? radius : radius * .45;
            double angle = Math.PI / 5 * i - Math.PI / 2;
            star.getPoints().addAll(Math.cos(angle) * r, Math.sin(angle) * r);
        }
        star.setFill(colour);
        star.setStroke(Theme.NAVY_950);
        star.setStrokeWidth(2);
        return star;
    }

    static Node lock() {
        var shackle = new Arc(0, -6, 9, 10, 0, 180);
        shackle.setType(ArcType.OPEN);
        shackle.setFill(null);
        shackle.setStroke(Theme.TEXT_SOFT);
        shackle.setStrokeWidth(4);
        var body = new Rectangle(-13, -6, 26, 20);
        body.setArcWidth(8);
        body.setArcHeight(8);
        body.setFill(Theme.TEXT_SOFT);
        return new Group(shackle, body);
    }
}
