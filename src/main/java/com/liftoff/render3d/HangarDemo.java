package com.liftoff.render3d;

import com.liftoff.model.ModuleSlot;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.liftoff.model.ModuleSlot.*;

/** Standalone visual check and integration example. */
public final class HangarDemo {
    private static final List<KeyCode> SLOT_KEYS =
            List.of(KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5);
    private static final List<KeyCode> NUMBER_PAD =
            List.of(KeyCode.NUMPAD1, KeyCode.NUMPAD2, KeyCode.NUMPAD3, KeyCode.NUMPAD4, KeyCode.NUMPAD5);
    private static final List<String> SLOT_LABELS = List.of("Airframe", "Thrusters", "Power", "Cargo bay", "Payload");
    static final Map<ModuleSlot, List<String>> MODELS = Map.of(
            AIRFRAME, List.of("shuttle-orbiter", "x-37b", "dream-chaser", "sorce-bus", "eo-1-bus",
                    "landsat-7-bus", "orion", "mars-aeroshell"),
            THRUSTERS, List.of("shuttle-oms", "orion-main-engine", "rl10", "hydrazine", "nstar"),
            POWER, List.of("body-solar", "deployable-solar", "fuel-cell", "mmrtg"),
            CARGO_BAY, List.of("bay-3u", "bay-6u", "bay-12u", "shuttle-bay"),
            SATELLITE, List.of("cubesat-1u", "cubesat-3u", "cubesat-6u", "lunar-lander",
                    "mars-rover", "mars-helicopter"));

    private HangarDemo() {
    }

    public static void main(String[] args) {
        Application.launch(Window.class, args);
    }

    public static final class Window extends Application {
        @Override
        public void start(Stage stage) {
            stage.setTitle("LIFTOFF · 3D Hangar");
            stage.setMinWidth(720);
            stage.setMinHeight(480);
            stage.setScene(createScene());
            stage.show();
        }
    }

    static Scene createScene() {
        var view = new HangarShipView();
        var choices = new EnumMap<ModuleSlot, Integer>(ModuleSlot.class);
        var buttons = new EnumMap<ModuleSlot, Button>(ModuleSlot.class);
        var toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER);
        var bay = new ToggleButton();
        setBay(view, bay, false);
        bay.setOnAction(event -> setBay(view, bay, bay.isSelected()));
        var calm = new CheckBox("Calm mode");
        calm.setStyle("-fx-text-fill: #EDF4FF;");
        calm.selectedProperty().addListener((property, previous, enabled) -> view.setCalmMode(enabled));

        for (var slot : ModuleSlot.values()) {
            var button = new Button();
            buttons.put(slot, button);
            toolbar.getChildren().add(button);
            button.setOnAction(event -> {
                if (slot == SATELLITE) {
                    setBay(view, bay, true);
                }
                select(view, choices, button, slot);
            });
            select(view, choices, button, slot);
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
        }

        var title = new Label("LIFTOFF  /  3D HANGAR");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #E9FAFF;");
        var help = new Label("Drag to orbit   •   Scroll to zoom   •   1–5 swap parts   •   B toggles the bay");
        help.setStyle("-fx-text-fill: #B9C7E6;");
        var header = new VBox(4, title, help);
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            header.getChildren().add(new Label("JavaFX 3D requires a hardware-accelerated graphics pipeline."));
            header.getChildren().getLast().setStyle("-fx-text-fill: #FFB76B;");
        }
        header.setPadding(new Insets(12, 16, 11, 16));
        toolbar.getChildren().addAll(bay, calm);
        toolbar.setPadding(new Insets(12, 12, 12, 12));
        var root = new BorderPane(view.node(), header, null, toolbar, null);
        root.setStyle("-fx-background-color: #07122B; -fx-font-family: 'Segoe UI';");
        var scene = new Scene(root, 1060, 720);
        scene.setOnKeyPressed(event -> {
            int slotIndex = SLOT_KEYS.indexOf(event.getCode());
            if (slotIndex < 0) {
                slotIndex = NUMBER_PAD.indexOf(event.getCode());
            }
            if (slotIndex >= 0) {
                buttons.get(ModuleSlot.values()[slotIndex]).fire();
                event.consume();
            } else if (event.getCode() == KeyCode.B) {
                bay.fire();
                event.consume();
            }
        });
        view.node().setOnMouseMoved(event -> view.setDropHighlight(view.slotAt(event.getSceneX(), event.getSceneY())));
        view.node().setOnMouseExited(event -> view.setDropHighlight(null));
        return scene;
    }

    private static void select(HangarShipView view, Map<ModuleSlot, Integer> choices, Button button, ModuleSlot slot) {
        List<String> models = MODELS.get(slot);
        int index = (choices.getOrDefault(slot, -1) + 1) % models.size();
        choices.put(slot, index);
        String model = models.get(index);
        view.showPart(slot, model);
        button.setText((slot.ordinal() + 1) + "  " + SLOT_LABELS.get(slot.ordinal()) + "\n" + model.replace('-', ' '));
        style(button);
    }

    private static void setBay(HangarShipView view, ToggleButton button, boolean open) {
        view.setCargoBayOpen(open);
        button.setSelected(open);
        button.setText(open ? "B  Close bay" : "B  Open bay");
        style(button);
    }

    private static void style(Node button) {
        button.setStyle("-fx-background-color: #15325A; -fx-border-color: #33688C;"
                + "-fx-border-radius: 5; -fx-background-radius: 5; -fx-text-fill: #EDF4FF;");
    }
}
