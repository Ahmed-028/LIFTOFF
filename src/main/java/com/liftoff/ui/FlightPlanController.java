package com.liftoff.ui;

import com.liftoff.engine.DesignIssue;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.Orbit;
import com.liftoff.session.stub.Mission;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Arrays;
import java.util.function.Function;

/**
 * The flight plan, chosen before the ship is built: the rocket, the science orbit (Earth
 * missions) and how long the science lasts. Knowing the rocket first tells the player how
 * heavy the ship may be.
 */
public final class FlightPlanController extends MissionScreen {
    @FXML private ChoiceBox<LaunchVehicle> rocket;
    @FXML private Label rocketAbout;
    @FXML private Label rocketFacts;
    @FXML private VBox orbitGroup;
    @FXML private ChoiceBox<Orbit> orbit;
    @FXML private Label orbitAbout;
    @FXML private Label orbitFacts;
    @FXML private VBox durationGroup;
    @FXML private ChoiceBox<Integer> duration;
    @FXML private StackPane gaugeHost;
    @FXML private VBox readout;

    private BuildGauges gauges;
    private WarningList warnings;

    @Override
    protected void setUp() {
        var entry = mission();
        boolean earth = entry.destination() == Mission.Destination.EARTH;
        boolean variableDuration = entry.steps() == 0;
        var catalog = session.catalog();
        show(orbitGroup, earth);
        show(durationGroup, variableDuration);
        rocket.setConverter(names(LaunchVehicle::kidName));
        rocket.setTooltip(new Tooltip(text.get("supplies.chooseRocket")));
        rocket.getItems().setAll(catalog.launchVehicles().stream()
                .filter(vehicle -> Arrays.stream(classes(entry.destination()))
                        .anyMatch(launchClass -> vehicle.capacityFor(launchClass).isPresent())).toList());
        rocket.setValue(session.design().launchVehicle());
        rocket.setOnAction(event -> {
            if (rocket.getValue() != null) {
                session.chooseLaunchVehicle(rocket.getValue().id());
            }
        });
        if (earth) {
            orbit.setConverter(names(Orbit::kidName));
            orbit.getItems().setAll(catalog.orbits());
            orbit.setValue(session.design().orbit());
            orbit.setTooltip(new Tooltip(text.get("supplies.chooseOrbit")));
            orbit.setOnAction(event -> {
                if (orbit.getValue() != null) {
                    session.chooseOrbit(orbit.getValue().id());
                }
            });
        }
        if (variableDuration) {
            for (int months = entry.briefing().minScienceMonths(); months <= entry.briefing().deadlineMonths(); months++) {
                duration.getItems().add(months);
            }
            duration.setValue(session.design().scienceMonths());
            duration.setOnAction(event -> session.setScienceMonths(duration.getValue()));
        }
        gauges = new BuildGauges(text, true);
        warnings = new WarningList(text);
        gaugeHost.getChildren().setAll(gauges);
        readout.getChildren().add(warnings);
    }

    @Override
    protected void update() {
        var design = session.design();
        var vehicle = design.launchVehicle();
        show(rocketAbout, vehicle != null);
        show(rocketFacts, vehicle != null);
        if (vehicle != null) {
            rocketAbout.setText(vehicle.name() + " · " + vehicle.description());
            var lift = vehicle.capacityFor(launchClass(design.orbit()));
            String capacity = lift.isPresent() ? text.get("plan.rocketLift", lift.getAsDouble())
                    : text.get("plan.rocketNoLift");
            rocketFacts.setText(capacity + " · " + text.cost(vehicle.costMusd(), vehicle.costBasis()));
        }
        var chosen = design.orbit();
        show(orbitAbout, chosen != null);
        show(orbitFacts, chosen != null);
        if (chosen != null) {
            orbitAbout.setText(chosen.name() + " · " + chosen.description());
            orbitFacts.setText(text.get("plan.orbitFacts", chosen.altitudeKm(), chosen.inclinationDeg()));
        }
        gauges.refresh(session);
        warnings.getChildren().clear();
        warnings.addDesign(session.designIssues().stream()
                .filter(issue -> issue.kind() == DesignIssue.Kind.ORBIT_MISSES_TARGET).toList());
    }

    /** The class of orbit the rocket must reach: the chosen orbit's on Earth, else the destination's. */
    private LaunchClass launchClass(Orbit chosen) {
        return switch (mission().destination()) {
            case EARTH -> chosen == null ? LaunchClass.LOW_INCLINATION : chosen.launchClass();
            case MOON -> LaunchClass.TRANS_LUNAR;
            case MARS -> LaunchClass.TRANS_MARS;
        };
    }

    private static LaunchClass[] classes(Mission.Destination destination) {
        return switch (destination) {
            case EARTH -> new LaunchClass[] { LaunchClass.LOW_INCLINATION, LaunchClass.POLAR };
            case MOON -> new LaunchClass[] { LaunchClass.TRANS_LUNAR };
            case MARS -> new LaunchClass[] { LaunchClass.TRANS_MARS };
        };
    }

    private static <T> StringConverter<T> names(Function<T, String> label) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : label.apply(value);
            }

            @Override
            public T fromString(String value) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
