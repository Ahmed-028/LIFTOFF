package com.liftoff.engine.score;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

/**
 * One card of the report. The interface renders it from messages.properties with
 * {@code MessageFormat.format(text(messageKey), params.toArray())}.
 *
 * @param params the values for {0}, {1}, ... of the message: names (resource, unit, measurement) as
 *               enum names, numbers as Integer or Double
 */
public record WhyCard(String messageKey, List<Object> params) {

    public WhyCard {
        params = List.copyOf(params);
    }

    static WhyCard of(String messageKey, Object... params) {
        return new WhyCard(messageKey, List.of(params));
    }

    /** English fallback for when messages.properties has no entry for the key. */
    public String defaultText() {
        String pattern = ReportTexts.pattern(messageKey);
        return pattern == null ? messageKey : new MessageFormat(pattern, Locale.ENGLISH).format(params.toArray());
    }
}
