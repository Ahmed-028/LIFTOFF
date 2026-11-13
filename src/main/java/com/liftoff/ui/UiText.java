package com.liftoff.ui;

import com.liftoff.model.Requirement;
import com.liftoff.model.ValueBasis;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Region;

import java.text.MessageFormat;
import java.util.ResourceBundle;

record UiText(ResourceBundle bundle) {
    String get(String key, Object... values) {
        String pattern = bundle.getString(key);
        return values.length == 0 ? pattern : new MessageFormat(pattern, bundle.getLocale()).format(values);
    }

    String requirement(Requirement requirement) {
        String name = get("measurement." + requirement.type().name());
        return requirement.maxResolutionM() == null ? name
                : get("card.needs.withDetail", name, requirement.maxResolutionM());
    }

    String cost(double amount, ValueBasis basis) {
        return get(basis == ValueBasis.GAME_ESTIMATE ? "value.estimatedCost" : "value.cost", amount);
    }

    Label label(String content, String style) {
        var label = new Label(content);
        label.getStyleClass().add(style);
        wrap(label);
        return label;
    }

    static void wrap(Labeled label) {
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(Region.USE_PREF_SIZE);
    }
}
