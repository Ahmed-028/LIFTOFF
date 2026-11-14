package com.liftoff.engine.score;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;


public record WhyCard(String messageKey, List<Object> params) {

    public WhyCard {
        params = List.copyOf(params);
    }

    static WhyCard of(String messageKey, Object... params) {
        return new WhyCard(messageKey, List.of(params));
    }


    public String defaultText() {
        String pattern = ReportTexts.pattern(messageKey);
        return pattern == null ? messageKey : new MessageFormat(pattern, Locale.ENGLISH).format(params.toArray());
    }
}
