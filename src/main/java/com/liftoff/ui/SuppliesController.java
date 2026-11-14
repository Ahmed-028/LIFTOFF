package com.liftoff.ui;

import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.Objective;
import com.liftoff.model.ValueBasis;
import com.liftoff.session.MissionStep;
import com.liftoff.render.Icons;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Comparator;
import java.util.Locale;

public final class SuppliesController extends MissionScreen {
    private final PartPreview previews = new PartPreview();
    @FXML private VBox instruments;
    @FXML private VBox stations;
    @FXML private StackPane gaugeHost;
    @FXML private VBox readout;
    @FXML private VBox crewGroup;
    @FXML private Tab crewTab;
    @FXML private TextField supplies;
    @FXML private Label crewMinimum;
    @FXML private Label suppliesError;
    @FXML private Button review;
    @FXML private TabPane supplyTabs;

    private BuildGauges gauges;
    private WarningList warnings;

    @Override
    protected void setUp() {
        var entry = mission();
        var catalog = session.catalog();
        if (entry.crew() == 0) {
            show(crewGroup, false);
            supplyTabs.getTabs().remove(crewTab);
        }
        supplies.setText(Double.toString(session.crewSuppliesKg()));
        show(suppliesError, false);
        supplies.textProperty().addListener((property, previous, current) -> updateSupplies(current));

        var goal = entry.briefing();
        var groups = new EnumMap<ToolGroup, List<Instrument>>(ToolGroup.class);
        catalog.instruments().stream()
                .sorted(Comparator.comparingInt(item -> measurementPriority(item, goal)))
                .forEach(item -> groups.computeIfAbsent(ToolGroup.of(item), key -> new ArrayList<>()).add(item));

        groups.entrySet().stream()
                .sorted(Comparator.comparingInt(group -> measurementPriority(group.getValue().getFirst(), goal)))
                .forEach(group -> {
                    String key = "supplies.group." + group.getKey();
                    var section = new VBox(10, text.label(text.get(key), "tool-group-title"),
                            text.label(text.get(key + ".about"), "muted"));
                    section.setId("tools-" + group.getKey().name().toLowerCase(java.util.Locale.ROOT));
                    section.getStyleClass().add("tool-group");
                    group.getValue().forEach(instrument -> section.getChildren().add(instrumentCard(instrument, goal)));
                    instruments.getChildren().add(section);
                });
        for (GroundStation station : catalog.groundStations()) {
            var option = new CheckBox(station.kidName());
            option.setId("station-" + station.id());
            option.setSelected(session.design().hasStation(station.id()));
            option.setOnAction(event -> session.toggleStation(station.id()));
            var card = new ChoiceCard(option, text.get("supplies.picked"), text.get("supplies.pick"));
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


    private ChoiceCard instrumentCard(Instrument instrument, Objective goal) {
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
        card.add(text.label(instrument.description(), "body-text"),
                text.label(text.get("supplies.measures"), "discover-label"),
                ChoiceCard.chips(instrument.capabilities().stream()
                        .map(capability -> text.get("measurement." + capability.type())).distinct()
                        .map(name -> (Node) ChoiceCard.chip(name, null, "goal-tag")).toArray(Node[]::new)));
        card.addDetails("details-" + instrument.id(), text.get("supplies.moreDetails"),
                text.get("supplies.fewerDetails"), () -> instrumentDetails(instrument));
        return card;
    }

    // Show cited instrument values in the expanded details.
    private List<Node> instrumentDetails(Instrument instrument) {
        var content = new ArrayList<Node>();
        content.add(text.label(text.get("supplies.resolutionTitle"), "discover-label"));
        for (var capability : instrument.capabilities()) {
            String name = text.get("measurement." + capability.type());
            content.add(text.label(capability.resolutionM() == null ? text.get("supplies.resolutionUnknown", name)
                    : text.get("supplies.resolution", name, capability.resolutionM()), "body-text"));
        }
        content.add(text.label(text.get("supplies.facts"), "discover-label"));
        content.add(ChoiceCard.chips(
                ChoiceCard.chip(text.get("supplies.stat.mass", instrument.massKg()), Icons.mass(16), "stat-chip"),
                ChoiceCard.chip(basis("supplies.stat.power", instrument.avgPowerW(), instrument.powerBasis()),
                        Icons.power(16), "stat-chip"),
                ChoiceCard.chip(basis("supplies.stat.data", instrument.dataGbitPerDay(), instrument.dataBasis()),
                        Icons.data(16), "stat-chip"),
                ChoiceCard.chip(text.cost(instrument.costMusd(), instrument.costBasis()), Icons.coin(16), "stat-chip"),
                ChoiceCard.chip(basis("supplies.stat.build", instrument.buildMonths(), instrument.buildBasis()),
                        Icons.clock(16), "stat-chip")));
        content.add(text.label(text.get("supplies.estimateNote"), "muted"));
        content.add(text.label(text.get("supplies.source", instrument.source().title(), instrument.source().detail()),
                "muted"));
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

    // Prefix estimates with "about" so they are not mistaken for published values.
    private String basis(String key, double value, ValueBasis basis) {
        return (basis == ValueBasis.GAME_ESTIMATE ? "\u2248" : "") + text.get(key, value);
    }

    @Override
    protected void update() {
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

    private static int measurementPriority(Instrument instrument, Objective goal) {
        if (instrument.capabilities().stream().anyMatch(capability -> goal.required().stream()
                .anyMatch(requirement -> requirement.type() == capability.type()))) {
            return 0;
        }
        return instrument.capabilities().stream().anyMatch(capability -> goal.bonus().contains(capability.type())) ? 1 : 2;
    }

    private static void show(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }


    private enum ToolGroup {
        CAMERA, HEAT, MICROWAVE, LASER, GROUND, WEATHER;


        static ToolGroup of(Instrument instrument) {
            return switch (instrument.capabilities().getFirst().type()) {
                case LAND_IMAGING, CLOUD_IMAGING, LIGHTNING, MARS_SURFACE_IMAGING -> CAMERA;
                case THERMAL_IMAGING, ENERGY_BUDGET, LUNAR_THERMAL_MAPPING -> HEAT;
                case RAIN_MICROWAVE, SEA_ICE_MICROWAVE, MARS_SUBSURFACE_RADAR -> MICROWAVE;
                case ICE_HEIGHT_LASER, LUNAR_TOPOGRAPHY -> LASER;
                case LUNAR_SEISMOLOGY, MARS_SURFACE_COMPOSITION -> GROUND;
                case MARS_WEATHER -> WEATHER;
            };
        }
    }
}
