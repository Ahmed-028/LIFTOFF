package com.liftoff.ui;

import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.Objective;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.Orbit;
import com.liftoff.session.MissionStep;
import com.liftoff.session.stub.Mission;
import com.liftoff.session.stub.ModuleSlot;
import com.liftoff.session.stub.ShipPart;
import com.liftoff.render.Icons;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

public final class SuppliesController extends MissionScreen {
    private final PartPreview previews = new PartPreview();
    @FXML private VBox instruments;
    @FXML private VBox stations;
    @FXML private StackPane gaugeHost;
    @FXML private VBox readout;
    @FXML private ChoiceBox<LaunchVehicle> rocket;
    @FXML private VBox orbitGroup;
    @FXML private ChoiceBox<Orbit> orbit;
    @FXML private VBox durationGroup;
    @FXML private ChoiceBox<Integer> duration;
    @FXML private VBox crewGroup;
    @FXML private TextField supplies;
    @FXML private Label crewMinimum;
    @FXML private Label suppliesError;
    @FXML private Button review;
    @FXML private TabPane supplyTabs;
    @FXML private VBox spacecraftPreview;
    @FXML private ChoiceBox<ShipPart> airframe;
    @FXML private Button previousTab;
    @FXML private Button nextTab;

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
        show(crewGroup, entry.crew() > 0);

        rocket.setConverter(names(LaunchVehicle::kidName));
        rocket.setTooltip(new javafx.scene.control.Tooltip(text.get("supplies.chooseRocket")));
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
            orbit.setTooltip(new javafx.scene.control.Tooltip(text.get("supplies.chooseOrbit")));
            orbit.setOnAction(event -> {
                if (orbit.getValue() != null) {
                    session.chooseOrbit(orbit.getValue().id());
                }
            });
        }

        if (variableDuration) {
            int start = entry.briefing().minScienceMonths();
            int end = entry.briefing().deadlineMonths();
            for (int months = start; months <= end; months++) {
                duration.getItems().add(months);
            }
            duration.setValue(session.design().scienceMonths());
            duration.setOnAction(event -> session.setScienceMonths(duration.getValue()));
        }

        supplies.setText(Double.toString(session.crewSuppliesKg()));
        show(suppliesError, false);
        supplies.textProperty().addListener((property, previous, current) -> updateSupplies(current));

        var goal = entry.briefing();
        var options = catalog.instruments().stream()
                .sorted(Comparator.comparingInt(item -> measurementPriority(item, goal))).toList();
        for (var instrument : options) {
            var option = new CheckBox(instrument.kidName());
            option.setId("instrument-" + instrument.id());
            option.setSelected(session.design().instruments().contains(instrument));
            option.setOnAction(event -> {
                if (option.isSelected()) {
                    session.addInstrument(instrument.id());
                } else {
                    session.removeInstrument(instrument.id());
                }
            });
            var card = new ChoiceCard(option, text.get("supplies.picked"), text.get("supplies.pick"));
            Tooltip.install(card, new Tooltip(instrument.name()));
            String origin = instrument.flewOn() == null ? instrument.name()
                    : text.get("supplies.flewOn", instrument.name(), instrument.flewOn());
            card.add(text.label(origin, "muted"));
            int priority = measurementPriority(instrument, goal);
            if (priority < 2) {
                var tag = ChoiceCard.chip(text.get(priority == 0 ? "supplies.goal.required" : "supplies.goal.bonus"),
                        null, "goal-tag");
                tag.getStyleClass().add(priority == 0 ? "required" : "bonus");
                card.add(ChoiceCard.chips(tag));
            }
            card.addDetails("details-" + instrument.id(), text.get("supplies.moreDetails"),
                    text.get("supplies.fewerDetails"), () -> instrumentDetails(instrument));
            instruments.getChildren().add(card);
        }
        airframe.setConverter(names(ShipPart::kidName));
        airframe.getItems().setAll(catalog.partsFor(entry.id(), ModuleSlot.AIRFRAME));
        airframe.setValue(session.build().part(ModuleSlot.AIRFRAME).orElse(null));
        airframe.setOnAction(event -> {
            if (airframe.getValue() != null) {
                session.choosePart(airframe.getValue().id());
            }
        });
        supplyTabs.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> {
            previousTab.setDisable(index.intValue() == 0);
            nextTab.setDisable(index.intValue() == supplyTabs.getTabs().size() - 1);
        });
        previousTab.setDisable(true);

        for (GroundStation station : catalog.groundStations()) {
            var option = new CheckBox(station.kidName());
            option.setId("station-" + station.id());
            option.setSelected(session.design().hasStation(station.id()));
            option.setOnAction(event -> session.toggleStation(station.id()));
            var card = new ChoiceCard(option, text.get("supplies.picked"), text.get("supplies.pick"));
            Tooltip.install(card, new Tooltip(station.name()));
            card.add(text.label(station.name(), "muted"),
                    ChoiceCard.chips(ChoiceCard.chip(text.get("supplies.stationStats", station.bands(),
                            text.cost(station.costMusdPerMonth(), station.costBasis())), Icons.coin(16), "stat-chip")));
            stations.getChildren().add(card);
        }

        gauges = new BuildGauges(text, true);
        warnings = new WarningList(text);
        gaugeHost.getChildren().setAll(gauges);
        readout.getChildren().add(warnings);
    }

    /** What a science tool card shows once its "More details" link is opened. */
    private java.util.List<Node> instrumentDetails(Instrument instrument) {
        var content = new ArrayList<Node>();
        content.add(text.label(text.get("supplies.discover"), "discover-label"));
        content.add(text.label(instrument.description(), "body-text"));
        var tags = instrument.capabilities().stream().map(capability -> text.get("measurement." + capability.type()))
                .distinct().map(name -> (Node) ChoiceCard.chip(name, null, "goal-tag")).toArray(Node[]::new);
        content.add(ChoiceCard.chips(tags));
        content.add(ChoiceCard.chips(
                ChoiceCard.chip(text.get("supplies.stat.mass", instrument.massKg()), Icons.mass(16), "stat-chip"),
                ChoiceCard.chip(text.get("supplies.stat.power", instrument.avgPowerW()), Icons.power(16), "stat-chip"),
                ChoiceCard.chip(text.get("supplies.stat.data", instrument.dataGbitPerDay()), Icons.data(16), "stat-chip"),
                ChoiceCard.chip(text.cost(instrument.costMusd(), instrument.costBasis()), Icons.coin(16), "stat-chip")));
        String model = InstrumentVisual.model(instrument);
        boolean renderModel = model != null
                && javafx.application.Platform.isSupported(javafx.application.ConditionalFeature.SCENE3D);
        var picture = new VBox(6, renderModel ? previews.thumbnail(model, 260, 136) : InstrumentVisual.diagram(instrument),
                text.label(text.get(renderModel ? "supplies.nasaInstrument" : "supplies.schematic"), "muted"));
        var host = InstrumentVisual.hostImage(instrument);
        if (host != null) {
            picture.getChildren().addAll(host,
                    text.label(text.get("supplies.hostCredit", InstrumentVisual.hostName(instrument)), "muted"));
        }
        picture.getStyleClass().add("details-picture");
        content.add(picture);
        return content;
    }

    @FXML private void onAirframe() {
        onStep(MissionStep.SHIP);
    }

    @FXML private void onPreviousTab() {
        supplyTabs.getSelectionModel().selectPrevious();
    }

    @FXML private void onNextTab() {
        supplyTabs.getSelectionModel().selectNext();
    }

    @Override
    protected void update() {
        var selected = session.build().part(ModuleSlot.AIRFRAME).orElse(null);
        if (selected != null && !selected.equals(spacecraftPreview.getUserData())) {
            spacecraftPreview.setUserData(selected);
            spacecraftPreview.getChildren().setAll(previews.thumbnail(selected, 310, 175),
                    text.label(selected.kidName(), "section-title"), text.label(selected.description(), "muted"));
        }
        crewMinimum.setText(text.get("supplies.crewMinimum", session.requiredCrewSuppliesKg()));
        gauges.refresh(session);
        warnings.showBuildWithoutGauges(session.buildReport().issues());
    }

    private void updateSupplies(String input) {
        try {
            double mass = Double.parseDouble(input.trim().replace(',', '.'));
            if (!Double.isFinite(mass) || mass < 0) {
                throw new NumberFormatException();
            }
            session.setCrewSuppliesKg(mass);
            show(suppliesError, false);
            review.setDisable(false);
        } catch (NumberFormatException ex) {
            show(suppliesError, true);
            review.setDisable(true);
        }
    }

    @FXML
    @Override
    protected void onNext() {
        if (crewGroup.isVisible() && suppliesError.isVisible()) {
            supplies.requestFocus();
            return;
        }
        super.onNext();
    }

    @Override
    protected void onStep(MissionStep step) {
        if (step == MissionStep.REVIEW && crewGroup.isVisible() && suppliesError.isVisible()) {
            supplies.requestFocus();
            return;
        }
        super.onStep(step);
    }

    private static LaunchClass[] classes(Mission.Destination destination) {
        return switch (destination) {
            case EARTH -> new LaunchClass[] { LaunchClass.LOW_INCLINATION, LaunchClass.POLAR };
            case MOON -> new LaunchClass[] { LaunchClass.TRANS_LUNAR };
            case MARS -> new LaunchClass[] { LaunchClass.TRANS_MARS };
        };
    }

    private static int measurementPriority(Instrument instrument, Objective goal) {
        if (instrument.capabilities().stream().anyMatch(capability -> goal.required().stream()
                .anyMatch(requirement -> requirement.type() == capability.type()))) {
            return 0;
        }
        return instrument.capabilities().stream().anyMatch(capability -> goal.bonus().contains(capability.type())) ? 1 : 2;
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

    private static void show(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
