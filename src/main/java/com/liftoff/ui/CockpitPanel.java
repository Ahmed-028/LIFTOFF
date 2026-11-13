package com.liftoff.ui;

import com.liftoff.engine.ResourceType;
import com.liftoff.render3d.CabinView;
import com.liftoff.session.GameSession;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

final class CockpitPanel extends VBox {
    private static final List<String> STATIONS = List.of("aisle", "seat", "console", "viewport");
    private static final int[] GRAVITY_LEVELS = {1, 10, 50, 100};
    private static final List<String> LIGHT_MODES = List.of("normal", "night", "warp");

    private final CabinView cabin;
    private final UiText text;
    private final String route;
    private final Label stationLabel;
    private final Label status;
    private final Label fuel;
    private final Label telemetry;
    private final Button back;
    private final Button forward;
    private final Button gravityButton;
    private final Button lightsButton;
    private final Button radarButton;

    private final double plannedFuel;
    private final double massKg;
    private final double budgetMusd;
    private final double powerDrawW;
    private final double powerSupplyW;

    private int station;
    private int gravityIndex;
    private int lightIndex;
    private String lastPhase = "";
    private String lastTimeLeft = "";
    private double lastProgress;
    private String lastTelemetryNote = "";

    CockpitPanel(GameSession session, UiText text, boolean inFlight, String route) {
        super(5);
        this.text = text;
        this.route = route;
        var readings = session.missionResources();
        var build = session.buildReport();
        plannedFuel = build.propellantKg();
        massKg = readings.get(ResourceType.MASS).used();
        budgetMusd = readings.get(ResourceType.BUDGET).used();
        powerDrawW = build.powerDrawW();
        powerSupplyW = build.powerSupplyW();

        getStyleClass().add("cockpit-panel");
        stationLabel = text.label("", "eyebrow");
        stationLabel.setId("cabin-station");
        stationLabel.setAlignment(Pos.CENTER);
        stationLabel.setMinWidth(Region.USE_PREF_SIZE);
        stationLabel.setStyle("-fx-max-width: 175;");
        back = new Button(text.get("cockpit.back"));
        back.setMinWidth(Region.USE_PREF_SIZE);
        forward = new Button(text.get("cockpit.forward"));
        forward.setMinWidth(Region.USE_PREF_SIZE);
        forward.setId("cabin-forward");
        back.setOnAction(event -> move(-1));
        forward.setOnAction(event -> move(1));

        var tips = text.label(text.get("cockpit.tips"), "muted");
        tips.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tips, Priority.ALWAYS);

        var controls = new HBox(8, back, stationLabel, forward, tips);
        controls.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(controls);

        if (Platform.isSupported(ConditionalFeature.SCENE3D)) {
            cabin = new CabinView(inFlight);
            cabin.setMinHeight(140);
            cabin.setPrefHeight(210);
            VBox.setVgrow(cabin, Priority.ALWAYS);
            getChildren().add(cabin);
        } else {
            cabin = null;
            getChildren().add(text.label(text.get("cockpit.fallback"), "muted"));
        }

        gravityButton = new Button();
        gravityButton.setMinWidth(Region.USE_PREF_SIZE);
        gravityButton.setOnAction(event -> cycleGravity());
        lightsButton = new Button();
        lightsButton.setMinWidth(Region.USE_PREF_SIZE);
        lightsButton.setOnAction(event -> cycleLights());
        radarButton = new Button(text.get("cockpit.scan"));
        radarButton.setMinWidth(Region.USE_PREF_SIZE);
        radarButton.setOnAction(event -> scanSector());
        var hornButton = new Button(text.get("cockpit.horn"));
        hornButton.setMinWidth(Region.USE_PREF_SIZE);
        hornButton.setOnAction(event -> sendChime());

        var consoleBar = new HBox(8, gravityButton, lightsButton, radarButton, hornButton);
        consoleBar.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(consoleBar);

        status = text.label("", "section-title");
        fuel = text.label("", "reading-value");
        lastTelemetryNote = inFlight ? text.get("fly.standby") : "";
        telemetry = text.label(lastTelemetryNote, "muted");
        var routeLabel = text.label(route, "eyebrow");
        routeLabel.setMinWidth(Region.USE_PREF_SIZE);
        var fuelNote = text.label(text.get("cockpit.fuelNote"), "muted");
        fuelNote.setMinWidth(Region.USE_PREF_SIZE);
        var limits = text.label(text.get("cockpit.readings", massKg, budgetMusd, powerDrawW, powerSupplyW), "body-text");
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var headerRow = new HBox(10, routeLabel, spacer, fuelNote);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(new VBox(2, headerRow, status, fuel, limits, telemetry));

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                int step = event.isShiftDown() ? -1 : 1;
                goToStation((station + step + STATIONS.size()) % STATIONS.size());
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) {
                move(1);
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) {
                move(-1);
                event.consume();
            }
        });

        refreshInteractiveLabels();
        move(0);
    }

    private void move(int direction) {
        goToStation(Math.clamp(station + direction, 0, STATIONS.size() - 1));
    }

    private void goToStation(int target) {
        station = Math.clamp(target, 0, STATIONS.size() - 1);
        stationLabel.setText(text.get("cockpit.station." + STATIONS.get(station)));
        back.setDisable(station == 0);
        forward.setDisable(station == STATIONS.size() - 1);
        if (cabin != null) {
            cabin.station(station);
        }
    }

    private void cycleGravity() {
        gravityIndex = (gravityIndex + 1) % GRAVITY_LEVELS.length;
        refreshInteractiveLabels();
        syncConsoleDisplay();
    }

    private void cycleLights() {
        lightIndex = (lightIndex + 1) % LIGHT_MODES.size();
        refreshInteractiveLabels();
        if (cabin != null) {
            cabin.setLightMode(lightIndex);
        }
    }

    private void scanSector() {
        lastTelemetryNote = text.get("cockpit.scanPing");
        telemetry.setText(lastTelemetryNote);
        if (cabin != null) {
            cabin.triggerRadarPing();
        }
        syncConsoleDisplay();
    }

    private void sendChime() {
        lastTelemetryNote = text.get("cockpit.hornBeep");
        telemetry.setText(lastTelemetryNote);
        syncConsoleDisplay();
    }

    private void refreshInteractiveLabels() {
        String gravityDesc = text.get("cockpit.gravity." + GRAVITY_LEVELS[gravityIndex]);
        gravityButton.setText(text.get("cockpit.gravity", gravityDesc));
        String lightDesc = text.get("cockpit.lights." + LIGHT_MODES.get(lightIndex));
        lightsButton.setText(text.get("cockpit.lights", lightDesc));
    }

    void update(String phase, String timeLeft, double progress, boolean animate) {
        lastPhase = phase;
        lastTimeLeft = timeLeft;
        lastProgress = progress;
        String next = text.get("cockpit.status", phase, timeLeft);
        if (!next.equals(status.getText())) {
            status.setText(next);
        }
        double used = plannedFuel * Math.clamp(progress, 0, 1);
        fuel.setText(plannedFuel == 0 ? text.get("gauge.noBurn") : text.get("cockpit.fuel", used, plannedFuel - used));
        if (cabin != null) {
            syncConsoleDisplay();
            if (animate) {
                cabin.update(progress);
            }
        }
    }

    void telemetry(String readings) {
        lastTelemetryNote = readings;
        telemetry.setText(readings);
        syncConsoleDisplay();
    }

    private void syncConsoleDisplay() {
        if (cabin == null) {
            return;
        }
        double used = plannedFuel * Math.clamp(lastProgress, 0, 1);
        double left = Math.max(0, plannedFuel - used);
        String gravityDesc = text.get("cockpit.gravity." + GRAVITY_LEVELS[gravityIndex]);
        cabin.updateConsole(route, lastPhase, lastTimeLeft, used, left, plannedFuel,
                powerDrawW, powerSupplyW, massKg, budgetMusd,
                "Gravity: " + gravityDesc, lastTelemetryNote, lastProgress);
    }

    void addControl(Button action) {
        var control = new Button();
        control.setMinWidth(Region.USE_PREF_SIZE);
        control.textProperty().bind(action.textProperty());
        control.disableProperty().bind(action.disableProperty());
        control.setOnAction(event -> action.fire());
        ((HBox) getChildren().getFirst()).getChildren().add(control);
    }
}
