package com.liftoff.ui;

import com.liftoff.model.Destination;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.Objective;
import com.liftoff.model.Orbit;
import com.liftoff.model.ShipPart;
import com.liftoff.session.MissionStep;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SuppliesController extends MissionScreen {
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
    @FXML private VBox instrumentPreview;
    @FXML private VBox spacecraftPreview;
    @FXML private ChoiceBox<ShipPart> airframe;
    @FXML private Button previousTab;
    @FXML private Button nextTab;

    private BuildGauges gauges;
    private WarningList warnings;

    @Override
    protected void setUp() {
        var entry = mission();
        var catalog = session.catalog();
        show(orbitGroup, entry.destination() == Destination.EARTH_ORBIT);
        show(durationGroup, entry.steps() == 0);
        show(crewGroup, entry.crew() > 0);

        choices(rocket, catalog.launchVehicles().stream().filter(vehicle -> vehicle.offeredFor(entry.destination()))
                .toList(), session.design().launchVehicle(), LaunchVehicle::kidName,
                vehicle -> session.chooseLaunchVehicle(vehicle.id()));
        rocket.setTooltip(new Tooltip(text.get("supplies.chooseRocket")));
        if (orbitGroup.isVisible()) {
            choices(orbit, catalog.orbits(), session.design().orbit(), Orbit::kidName,
                    chosen -> session.chooseOrbit(chosen.id()));
            orbit.setTooltip(new Tooltip(text.get("supplies.chooseOrbit")));
        }
        choices(airframe, catalog.partsFor(entry.id(), ModuleSlot.AIRFRAME),
                session.build().part(ModuleSlot.AIRFRAME).orElse(null), ShipPart::kidName,
                part -> session.choosePart(part.id()));
        if (durationGroup.isVisible()) {
            setUpDuration(entry.briefing());
        }
        supplies.setText(Double.toString(session.crewSuppliesKg()));
        show(suppliesError, false);
        supplies.textProperty().addListener((property, previous, current) -> updateSupplies(current));

        setUpInstruments(entry.briefing());
        catalog.groundStations().forEach(this::addStation);
        supplyTabs.getSelectionModel().selectedIndexProperty().addListener((property, before, index) -> {
            previousTab.setDisable(index.intValue() == 0);
            nextTab.setDisable(index.intValue() == supplyTabs.getTabs().size() - 1);
        });
        previousTab.setDisable(true);

        gauges = BuildGauges.column(text);
        warnings = new WarningList(text);
        gaugeHost.getChildren().setAll(gauges);
        readout.getChildren().add(warnings);
    }

    /** Fills a choice box, shows the current choice, and passes each new choice to the session. */
    private static <T> void choices(ChoiceBox<T> box, List<T> items, T current, Function<T, String> name,
                                    Consumer<T> choose) {
        box.setConverter(names(name));
        box.getItems().setAll(items);
        box.setValue(current);
        box.setOnAction(event -> {
            if (box.getValue() != null) {
                choose.accept(box.getValue());
            }
        });
    }

    private void setUpDuration(Objective goal) {
        for (int months = goal.minScienceMonths(); months <= goal.deadlineMonths(); months++) {
            duration.getItems().add(months);
        }
        duration.setValue(session.design().scienceMonths());
        duration.setOnAction(event -> session.setScienceMonths(duration.getValue()));
    }

    /** One check box per instrument, the most useful to the goal first. */
    private void setUpInstruments(Objective goal) {
        var options = session.catalog().instruments().stream()
                .sorted(Comparator.comparingInt(goal::relevanceOf)).toList();
        options.forEach(this::addInstrument);
        if (!options.isEmpty()) {
            showInstrument(session.design().instruments().stream().findFirst().orElse(options.getFirst()));
        }
    }

    private void addInstrument(Instrument instrument) {
        var option = new CheckBox(instrument.kidName());
        option.setId("instrument-" + instrument.id());
        option.setSelected(session.design().instruments().contains(instrument));
        option.setWrapText(true);
        option.setOnAction(event -> {
            showInstrument(instrument);
            if (option.isSelected()) {
                session.addInstrument(instrument.id());
            } else {
                session.removeInstrument(instrument.id());
            }
        });
        option.focusedProperty().addListener((property, before, focused) -> {
            if (focused) {
                showInstrument(instrument);
            }
        });
        option.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> showInstrument(instrument));
        String measurements = instrument.capabilities().stream()
                .map(capability -> text.get("measurement." + capability.type()))
                .distinct().collect(Collectors.joining(" · "));
        instruments.getChildren().addAll(option, text.label(measurements, "body-text"),
                text.label(text.get("supplies.instrumentStats", instrument.massKg(), instrument.avgPowerW(),
                        instrument.dataGbitPerDay(), text.cost(instrument.costMusd(), instrument.costBasis())), "muted"));
    }

    private void addStation(GroundStation station) {
        var option = new CheckBox(station.kidName());
        option.setId("station-" + station.id());
        option.setSelected(session.design().hasStation(station.id()));
        option.setOnAction(event -> session.toggleStation(station.id()));
        stations.getChildren().addAll(option, text.label(text.get("supplies.stationStats", station.bands(),
                text.cost(station.costMusdPerMonth(), station.costBasis())), "muted"));
    }

    private void showInstrument(Instrument instrument) {
        String model = InstrumentVisual.model(instrument);
        boolean renderModel = model != null && javafx.application.Platform.isSupported(javafx.application.ConditionalFeature.SCENE3D);
        instrumentPreview.getChildren().setAll(text.label(instrument.name(), "section-title"),
                renderModel ? PartPreview.thumbnail(model, 240, 126) : InstrumentVisual.diagram(instrument),
                text.label(text.get(renderModel ? "supplies.nasaInstrument" : "supplies.schematic"), "muted"),
                text.label(text.get("supplies.instrumentStats", instrument.massKg(), instrument.avgPowerW(),
                        instrument.dataGbitPerDay(), text.cost(instrument.costMusd(), instrument.costBasis())), "reading-value"));
        var host = InstrumentVisual.hostImage(instrument);
        if (host != null) {
            instrumentPreview.getChildren().addAll(host,
                    text.label(text.get("supplies.hostCredit", InstrumentVisual.hostName(instrument)), "muted"));
        }
        instrumentPreview.getChildren().add(text.label(instrument.description(), "body-text"));
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
            spacecraftPreview.getChildren().setAll(PartPreview.thumbnail(selected, 310, 175),
                    text.label(selected.kidName(), "section-title"), text.label(selected.description(), "muted"));
        }
        crewMinimum.setText(text.get("supplies.crewMinimum", session.requiredCrewSuppliesKg()));
        gauges.refresh(session);
        warnings.showBuild(session.buildReport().issues());
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
