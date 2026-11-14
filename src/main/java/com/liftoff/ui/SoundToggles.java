package com.liftoff.ui;

import com.liftoff.audio.Audio;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Two small switches for the music and the sound effects. Each says "on" or "off" in words. */
final class SoundToggles {

    private SoundToggles() {
    }

    static HBox create(UiText text) {
        var audio = Audio.get();
        var row = new HBox(6,
                toggle(text, "music", audio::musicOn, audio::setMusicOn),
                toggle(text, "effects", audio::soundOn, audio::setSoundOn));
        row.setMinWidth(Region.USE_PREF_SIZE);
        row.getStyleClass().add("sound-toggles");
        return row;
    }

    private static ToggleButton toggle(UiText text, String kind, BooleanSupplier on, Consumer<Boolean> set) {
        var toggle = new ToggleButton();
        toggle.setId(kind + "-toggle");
        toggle.getStyleClass().addAll("secondary", "sound-toggle");
        toggle.setMinWidth(Region.USE_PREF_SIZE);
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
