package com.liftoff.ui;

import com.liftoff.audio.Audio;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;


final class SoundToggles {

    private SoundToggles() {
    }

    static VBox create(UiText text) {
        var audio = Audio.get();
        var row = new VBox(8,
                toggle(text, "music", audio::musicOn, audio::setMusicOn),
                toggle(text, "effects", audio::soundOn, audio::setSoundOn));
        row.getStyleClass().add("sound-toggles");
        return row;
    }

    private static ToggleButton toggle(UiText text, String kind, BooleanSupplier on, Consumer<Boolean> set) {
        var toggle = new ToggleButton();
        toggle.setId(kind + "-toggle");
        toggle.getStyleClass().addAll("secondary", "sound-toggle");
        toggle.setMaxWidth(Double.MAX_VALUE);
        toggle.setSelected(on.getAsBoolean());
        Runnable label = () -> {
            String state = text.get(toggle.isSelected() ? "sound.on" : "sound.off");
            toggle.setText(text.get("sound." + kind, state));
            toggle.setAccessibleText(text.get("sound." + kind + ".accessible", state));
        };
        toggle.setTooltip(new Tooltip(text.get("sound." + kind + ".hint")));
        toggle.selectedProperty().addListener((property, before, selected) -> {
            set.accept(selected);
            label.run();
        });
        label.run();
        return toggle;
    }
}
