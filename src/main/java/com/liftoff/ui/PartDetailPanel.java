package com.liftoff.ui;

import com.liftoff.render.Icons;
import com.liftoff.session.stub.ShipPart;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class PartDetailPanel extends VBox {
    private final UiText text;
    private final PartPreview preview;

    PartDetailPanel(UiText text, PartPreview preview) {
        super(6);
        this.text = text;
        this.preview = preview;
        getStyleClass().add("part-detail");
    }

    void show(ShipPart part) {
        if (part == null) {
            getChildren().setAll(text.label(text.get("ship.noPart"), "muted"));
            return;
        }
        var image = new StackPane(preview.thumbnail(part, 190, 90));
        image.getStyleClass().add("part-picture");
        getChildren().setAll(text.label(part.kidName(), "section-title"), image,
                row(text.get("detail.mass", part.massKg()), Icons.mass(17)),
                row(text.get("detail.power", part.powerSupplyW(), part.powerDrawW()), Icons.power(17)),
                row(text.get("detail.data", part.dataGbitPerDay()), Icons.data(17)),
                row(text.get("detail.cost", text.cost(part.costMusd(), part.costBasis())), Icons.coin(17)));
        switch (part.slot()) {
            case AIRFRAME -> {
                addReading("detail.cargoLimit", part.payloadMassKg());
                addReading("detail.tank", part.propellantCapacityKg());
            }
            case THRUSTERS -> addReading("detail.thruster", part.thrustN(), part.specificImpulseS());
            case CARGO_BAY -> {
                addReading("detail.bay", part.bayLengthM(), part.bayDiameterM());
                addReading("detail.cargoLimit", part.payloadMassKg());
                addReading("detail.tank", part.propellantCapacityKg());
            }
            case SATELLITE -> {
                addReading("detail.payloadSize", part.lengthM(), part.diameterM());
                addReading("detail.tank", part.propellantCapacityKg());
            }
            case POWER -> { }
        }
        getChildren().add(text.label(part.description(), "muted"));
    }

    private void addReading(String key, Object... values) {
        getChildren().add(text.label(text.get(key, values), "detail-row"));
    }

    private Node row(String content, Node icon) {
        var label = text.label(content, "detail-row");
        label.setGraphic(icon);
        return label;
    }
}
