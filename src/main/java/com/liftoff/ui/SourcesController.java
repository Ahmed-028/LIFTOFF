package com.liftoff.ui;

import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.app.ScreenController;
import com.liftoff.model.SourceRef;
import com.liftoff.model.ValueBasis;
import com.liftoff.session.GameSession;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;
import java.util.function.Consumer;

public final class SourcesController implements ScreenController {
    @FXML private VBox categories;
    @FXML private Label linkStatus;
    private SceneNavigator navigator;
    private UiText text;

    @Override
    public void attach(SceneNavigator navigator, GameSession session, ResourceBundle messages) {
        this.navigator = navigator;
        text = new UiText(messages);
        var catalog = session.catalog();
        category("missions", rows -> catalog.missions().forEach(mission -> {
            var card = card(mission.briefing().title());
            cite(card, mission.source(), text.get("sources.scenario"));
            cite(card, mission.deltaVSource(), field("sources.deltaV", mission.deltaVBasis()));
            cite(card, mission.suppliesSource(), text.get("sources.crewRate"));
            cite(card, mission.downlinkSource(), text.get("sources.downlink"));
            rows.getChildren().add(card);
        }));
        category("parts", rows -> catalog.shipParts().forEach(part ->
                add(rows, part.name(), part.source(), field("sources.cost", part.costBasis()))));
        category("instruments", rows -> catalog.instruments().forEach(instrument ->
                add(rows, instrument.name(), instrument.source(), String.join(" · ",
                        field("sources.power", instrument.powerBasis()), field("sources.data", instrument.dataBasis()),
                        field("sources.cost", instrument.costBasis()), field("sources.buildTime", instrument.buildBasis())))));
        category("rockets", rows -> catalog.launchVehicles().forEach(rocket ->
                add(rows, rocket.name(), rocket.source(), field("sources.cost", rocket.costBasis()))));
        category("orbits", rows -> catalog.orbits().forEach(orbit ->
                add(rows, orbit.name(), orbit.source(), text.get("basis.PUBLISHED"))));
        category("stations", rows -> catalog.groundStations().forEach(station ->
                add(rows, station.name(), station.source(), field("sources.cost", station.costBasis()))));
        category("buses", rows -> catalog.buses().forEach(bus ->
                add(rows, bus.name(), bus.source(), String.join(" · ", field("sources.mass", bus.massBasis()),
                        field("sources.payload", bus.payloadBasis()), field("sources.cost", bus.costBasis()),
                        field("sources.buildTime", bus.buildBasis())))));
        category("events", rows -> catalog.events().forEach(event ->
                add(rows, text.get(event.messageKey()), event.source(), event.realExample())));
    }

    private void category(String key, Consumer<VBox> populate) {
        var rows = new VBox(10);
        rows.getStyleClass().add("source-rows");
        populate.accept(rows);
        var group = new TitledPane(text.get("sources." + key), rows);
        group.setExpanded(false);
        categories.getChildren().add(group);
    }

    private VBox card(String name) {
        var card = new VBox(5, text.label(name, "section-title"));
        card.getStyleClass().add("source-card");
        return card;
    }

    private void add(VBox rows, String name, SourceRef source, String basis) {
        var card = card(name);
        cite(card, source, basis);
        rows.getChildren().add(card);
    }

    private void cite(VBox card, SourceRef source, String basis) {
        if (source == null) {
            return;
        }
        var link = new Hyperlink(source.title());
        UiText.wrap(link);
        link.setOnAction(event -> {
            navigator.openSource(source.url());
            linkStatus.setText(text.get("sources.opened", source.title()));
        });
        card.getChildren().addAll(text.label(basis, "muted"), link, text.label(source.detail(), "body-text"));
    }

    private String field(String label, ValueBasis basis) {
        return text.get(label) + " · " + text.get("basis." + basis);
    }

    @FXML
    private void onBack() {
        navigator.show(Screen.MISSION_SELECT);
    }
}
