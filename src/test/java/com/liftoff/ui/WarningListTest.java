package com.liftoff.ui;

import com.liftoff.app.LiftoffApp;
import com.liftoff.engine.BuildIssue;
import com.liftoff.model.ModuleSlot;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WarningListTest {
    private final UiText text = new UiText(ResourceBundle.getBundle(LiftoffApp.MESSAGES_BUNDLE, Locale.ENGLISH));

    @Test
    void explainsMissingSlotsAndFormatsPowerWithUnits() {
        assertEquals("Choose a part for Cargo bay.", WarningList.describe(text,
                new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.CARGO_BAY, 1, 0)));
        assertEquals("The ship needs 160 W. Its power system gives 115 W.", WarningList.describe(text,
                new BuildIssue(BuildIssue.Kind.POWER_SHORT, ModuleSlot.POWER, 160, 115)));
    }

    @Test
    void everyWarningKindHasACompleteMessage() {
        for (var kind : BuildIssue.Kind.values()) {
            var slot = kind == BuildIssue.Kind.OVER_BUDGET || kind == BuildIssue.Kind.SUPPLIES_SHORT
                    ? null : ModuleSlot.AIRFRAME;
            String message = WarningList.describe(text, new BuildIssue(kind, slot, 6000, 3000));
            assertFalse(message.isBlank() || message.contains("{"), kind.name());
        }
    }
}
