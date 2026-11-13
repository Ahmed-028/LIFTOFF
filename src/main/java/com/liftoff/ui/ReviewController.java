package com.liftoff.ui;

import com.liftoff.model.Destination;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipPart;
import com.liftoff.render.Icons;
import com.liftoff.session.MissionStep;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class ReviewController extends MissionScreen {
    @FXML private VBox selectedParts;
    @FXML private VBox inspection;
    @FXML private StackPane confirmGate;
    @FXML private Button confirm;
    private BuildGauges gauges;
    private WarningList warnings;
    private BuildConfirmation confirmation;

    @Override
    protected void setUp() {
        gauges = BuildGauges.column(text);
        warnings = new WarningList(text);
        confirmation = new BuildConfirmation(confirmGate, confirm);
        inspection.getChildren().addAll(gauges, warnings);
        for (var slot : ModuleSlot.values()) {
            String name = session.build().part(slot).map(ShipPart::kidName).orElse(text.get("ship.empty"));
            var label = text.label(text.get("step.accessible", text.get("slot." + slot), name), "detail-row");
            label.setGraphic(Icons.forModule(slot, 18));
            selectedParts.getChildren().add(label);
        }
        var design = session.design();
        if (design.launchVehicle() != null) {
            selectedParts.getChildren().add(text.label(text.get("step.accessible",
                    text.get("supplies.rocket"), design.launchVehicle().kidName()), "detail-row"));
        } else {
            selectedParts.getChildren().add(text.label(text.get("review.noRocket"), "warning"));
        }
        if (design.orbit() != null) {
            selectedParts.getChildren().add(text.label(text.get("step.accessible",
                    text.get("supplies.orbit"), design.orbit().kidName()), "detail-row"));
        } else if (mission().destination() == Destination.EARTH_ORBIT) {
            selectedParts.getChildren().add(text.label(text.get("review.noOrbit"), "warning"));
        }
    }

    @Override
    protected void update() {
        var report = session.buildReport();
        gauges.refresh(session);
        warnings.show(report.issues(), session.designIssues());
        confirm.setText(text.get(session.isUnlocked(MissionStep.LAUNCH) ? "review.launch" : "review.confirm"));
        confirmation.refresh(session, text);
    }

    @FXML
    private void onConfirm() {
        if (!session.isUnlocked(MissionStep.LAUNCH)) {
            session.confirmBuild();
        }
        onNext();
    }

    @Override
    public void dispose() {
        confirmation.hideWarning();
        super.dispose();
    }
}
