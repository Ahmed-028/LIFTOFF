package com.liftoff.ui;

import com.liftoff.engine.ResourceType;
import com.liftoff.render3d.CabinView;
import com.liftoff.session.GameSession;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

final class CockpitPanel extends VBox {
    private static final List<String> STATIONS = List.of("aisle", "seat", "console");
    private final CabinView cabin;
    private final UiText text;
    private final Label stationLabel;
    private final Label status;
    private final Label fuel;
    private final Label telemetry;
    private final Button back;
    private final Button forward;
    private final double plannedFuel;
    private int station;

    CockpitPanel(GameSession session, UiText text, boolean inFlight, String route) {
        super(6);
        this.text = text;
        plannedFuel = session.buildReport().propellantKg();
        getStyleClass().add("cockpit-panel");
        stationLabel = text.label("", "eyebrow");
        back = new Button(text.get("cockpit.back"));
        forward = new Button(text.get("cockpit.forward"));
        forward.setId("cabin-forward");
        stationLabel.setId("cabin-station");
        back.setOnAction(event -> move(-1));
        forward.setOnAction(event -> move(1));
        var controls = new HBox(8, back, stationLabel, forward);
        controls.setAlignment(javafx.geometry.Pos.CENTER);
        getChildren().add(controls);
        if (Platform.isSupported(ConditionalFeature.SCENE3D)) {
            cabin = new CabinView(inFlight);
            cabin.setMinHeight(175);
            cabin.setPrefHeight(280);
            VBox.setVgrow(cabin, Priority.ALWAYS);
            getChildren().add(cabin);
        } else {
            cabin = null;
            getChildren().add(text.label(text.get("cockpit.fallback"), "muted"));
        }

        status = text.label("", "section-title");
        fuel = text.label("", "reading-value");
        telemetry = text.label("", "muted");
        var readings = session.missionResources();
        var build = session.buildReport();
        var limits = text.label(text.get("cockpit.readings", readings.get(ResourceType.MASS).used(),
                readings.get(ResourceType.BUDGET).used(), build.powerDrawW(), build.powerSupplyW()), "body-text");
        getChildren().add(new VBox(3, text.label(route, "eyebrow"), status, fuel, limits, telemetry,
                text.label(text.get("cockpit.fuelNote"), "muted")));
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT) {
                move(event.getCode() == KeyCode.RIGHT ? 1 : -1);
                event.consume();
            }
        });
        move(0);
    }

    private void move(int direction) {
        station = Math.clamp(station + direction, 0, STATIONS.size() - 1);
        stationLabel.setText(text.get("cockpit.station." + STATIONS.get(station)));
        back.setDisable(station == 0);
        forward.setDisable(station == STATIONS.size() - 1);
        if (cabin != null) {
            cabin.station(station);
        }
    }

    void update(String phase, String timeLeft, double progress, boolean animate) {
        String next = text.get("cockpit.status", phase, timeLeft);
        if (!next.equals(status.getText())) {
            status.setText(next);
        }
        double used = plannedFuel * Math.clamp(progress, 0, 1);
        fuel.setText(plannedFuel == 0 ? text.get("gauge.noBurn") : text.get("cockpit.fuel", used, plannedFuel - used));
        if (cabin != null && animate) {
            cabin.update(progress);
        }
    }

    void telemetry(String readings) {
        telemetry.setText(readings);
    }

    void addControl(Button action) {
        var control = new Button();
        control.textProperty().bind(action.textProperty());
        control.disableProperty().bind(action.disableProperty());
        control.setOnAction(event -> action.fire());
        ((HBox) getChildren().getFirst()).getChildren().add(control);
    }
}
