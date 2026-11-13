package com.liftoff.ui;

import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipPart;
import com.liftoff.render3d.HangarShipView;
import com.liftoff.render3d.ShipView;
import com.liftoff.session.MissionStep;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

public final class ShipBuilderController extends MissionScreen {
    @FXML private StackPane tabsHost;
    @FXML private StackPane partsHost;
    @FXML private StackPane gaugesHost;
    @FXML private StackPane hangarHost;
    @FXML private Label hangarHint;
    @FXML private VBox inspection;
    @FXML private CheckBox calmMode;
    @FXML private StackPane suppliesGate;
    @FXML private Button supplies;
    @FXML private StackPane confirmGate;
    @FXML private Button confirm;

    private ShipView shipView;
    private ModuleTabs tabs;
    private PartStrip strip;
    private PartDetailPanel details;
    private BuildGauges gauges;
    private WarningList warnings;
    private BuildConfirmation confirmation;
    private ModuleSlot active = ModuleSlot.AIRFRAME;
    private ShipPart focused;

    @Override
    protected void setUp() {
        if (Platform.isSupported(ConditionalFeature.SCENE3D)) {
            var hangar = new HangarShipView();
            calmMode.selectedProperty().addListener((property, previous, enabled) -> hangar.setCalmMode(enabled));
            shipView = hangar;
        } else {
            shipView = new SlotShipView(text);
            calmMode.setDisable(true);
        }
        Node viewport = shipView.node();
        viewport.setId("ship-viewport");
        viewport.setOnDragOver(this::dragOver);
        viewport.setOnDragDropped(this::drop);
        viewport.setOnDragExited(event -> clearHighlight());
        hangarHost.getChildren().addFirst(viewport);
        if (!(shipView instanceof HangarShipView)) {
            hangarHint.setText(text.get("ship.fallbackHint"));
        }

        tabs = new ModuleTabs(text, this::showSlot);
        strip = new PartStrip(text, this::choose, this::focus, this::clearHighlight);
        details = new PartDetailPanel(text);
        gauges = BuildGauges.row(text);
        warnings = new WarningList(text);
        confirmation = new BuildConfirmation(confirmGate, confirm);
        tabsHost.getChildren().setAll(tabs);
        partsHost.getChildren().setAll(strip);
        gaugesHost.getChildren().setAll(gauges);
        inspection.getChildren().addAll(details, warnings);
        showSlot(active);
    }

    @Override
    protected void update() {
        for (ModuleSlot slot : ModuleSlot.values()) {
            shipView.showPart(slot, session.build().part(slot)
                    .map(part -> slot == ModuleSlot.SATELLITE ? PartPreview.modelFor(part) : part.modelId())
                    .orElse(null));
        }
        tabs.refresh(active, session.build());
        strip.select(selectedId());
        gauges.refresh(session);
        var report = session.buildReport();
        warnings.showBuild(report.issues());

        var suppliesLocked = session.reasonLocked(MissionStep.SUPPLIES);
        supplies.setDisable(suppliesLocked.isPresent());
        String reason = suppliesLocked.map(text::get).orElse(null);
        var tooltip = supplies.getTooltip();
        if (!java.util.Objects.equals(reason, tooltip == null ? null : tooltip.getText())) {
            Tooltip.uninstall(suppliesGate, tooltip);
            supplies.setTooltip(reason == null ? null : new Tooltip(reason));
            Tooltip.install(suppliesGate, supplies.getTooltip());
        }

        confirmation.refresh(session, text);
    }

    @FXML
    private void onConfirm() {
        move(() -> {
            session.goTo(MissionStep.REVIEW);
            session.confirmBuild();
        });
    }

    private void showSlot(ModuleSlot slot) {
        active = slot;
        var parts = session.catalog().partsFor(mission().id(), slot);
        tabs.refresh(active, session.build());
        strip.setParts(parts, selectedId());
        focus(session.build().part(slot).orElse(parts.isEmpty() ? null : parts.getFirst()));
        shipView.setCargoBayOpen(slot == ModuleSlot.SATELLITE);
    }

    private void focus(ShipPart part) {
        if (!Objects.equals(part, focused) || details.getChildren().isEmpty()) {
            focused = part;
            details.show(part);
        }
    }

    private String selectedId() {
        return session.build().part(active).map(ShipPart::id).orElse(null);
    }

    private void choose(ShipPart part) {
        focus(part);
        if (!part.id().equals(selectedId())) {
            session.choosePart(part.id());
        }
    }

    /** The card being dragged, if it is one of the cards of the open tab. */
    private ShipPart draggedPart(Object partId) {
        return partId instanceof String id ? session.catalog().partsFor(mission().id(), active).stream()
                .filter(part -> part.id().equals(id)).findFirst().orElse(null) : null;
    }

    private void dragOver(DragEvent event) {
        var part = draggedPart(event.getDragboard().getContent(PartCard.PART_ID));
        var target = part == null ? null : shipView.slotAt(event.getSceneX(), event.getSceneY());
        shipView.setDropHighlight(target);
        if (part != null && target == part.slot()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void drop(DragEvent event) {
        boolean fitted = dropPart(event.getDragboard().getContent(PartCard.PART_ID),
                shipView.slotAt(event.getSceneX(), event.getSceneY()));
        clearHighlight();
        event.setDropCompleted(fitted);
        event.consume();
    }

    /**
     * What a drop does: on the place that matches the dragged card, the same as a click on the
     * card; anywhere else, nothing.
     *
     * @param partId what the drag carries
     * @param target the place on the ship under the pointer, or null if there is none
     * @return true if the part was fitted
     */
    boolean dropPart(Object partId, ModuleSlot target) {
        var part = draggedPart(partId);
        if (part == null || target != part.slot()) {
            return false;
        }
        choose(part);
        return true;
    }

    ShipView shipView() {
        return shipView;
    }

    private void clearHighlight() {
        shipView.setDropHighlight(null);
    }

    @Override
    public void dispose() {
        confirmation.hideWarning();
        shipView.dispose();
        super.dispose();
    }
}
