package com.liftoff.render;

import com.liftoff.session.stub.Mission;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;


public final class Theme {


    public static final Color NAVY_950 = Color.web("#050E26");
    public static final Color NAVY_900 = Color.web("#071330");
    public static final Color NAVY_800 = Color.web("#0D1E42");
    public static final Color NAVY_700 = Color.web("#16305E");
    public static final Color NAVY_600 = Color.web("#21427A");
    public static final Color SHELL_LINE = Color.web("#33609A");
    public static final Color SHELL_LINE_STRONG = Color.web("#4F7FBF");
    public static final Color TEXT = Color.web("#F3F8FF");
    public static final Color TEXT_SOFT = Color.web("#C3D3EE");


    public static final Color CYAN = Color.web("#22E3F2");
    public static final Color CYAN_SOFT = Color.web("#7EF4FB");

    public static final Color ORANGE = Color.web("#FF7A1A");
    public static final Color ORANGE_HIGH = Color.web("#FF9C3D");
    public static final Color ORANGE_INK = Color.web("#1A1206");

    public static final Color AMBER = Color.web("#FFCB69");
    public static final Color ALERT = Color.web("#FFACA4");
    public static final Color GOOD = Color.web("#8BE07A");


    public static final Color CARD = Color.web("#F2F8FF");
    public static final Color CARD_HOVER = Color.web("#FFFFFF");
    public static final Color CARD_INK = Color.web("#0B1A38");
    public static final Color CARD_SOFT = Color.web("#3D4E70");
    public static final Color CARD_LINE = Color.web("#C4D4EA");
    public static final Color CARD_PICKED = Color.web("#E0FCFF");
    public static final Color CARD_RING = Color.web("#0FB7C6");
    public static final Color CHIP = Color.web("#DCE8F7");
    public static final Color CHIP_INK = Color.web("#16305E");


    public static final Color HULL_WHITE = Color.web("#F2F5FA");
    public static final Color THERMAL_TILE = Color.web("#2A3040");
    public static final Color SOLAR_BLUE = Color.web("#2F64CF");
    public static final Color GLOW_CYAN = Color.web("#2BE6F5");


    public static final String FONT_BODY = "Barlow";
    public static final String FONT_DISPLAY = "Barlow Condensed";


    public static final double SPACE_XS = 4;
    public static final double SPACE_S = 8;
    public static final double SPACE_M = 12;
    public static final double SPACE_L = 18;
    public static final double SPACE_XL = 28;


    public static final double RADIUS_CHIP = 12;
    public static final double RADIUS_CARD = 18;
    public static final double RADIUS_PANEL = 22;

    public static final double MIN_TARGET = 44;


    public static final Duration MOTION_QUICK = Duration.millis(180);
    public static final Duration MOTION_SOFT = Duration.millis(420);

    public static final Duration MOTION_TRAVEL = Duration.millis(1600);

    private Theme() {
    }


    public enum World {
        HANGAR("", "#12285A", "#071330", "#0D1E42", "#16305E", "#33609A", "#22E3F2", "#C3D3EE"),
        EARTH("world-earth", "#0E6582", "#08303F", "#0B4A5E", "#0F6F86", "#2E9DB4", "#8BE07A", "#CDEFF8"),
        MOON("world-moon", "#2D3478", "#121334", "#252B66", "#343C88", "#5B62C9", "#D9DEEC", "#D7DCF5"),
        MARS("world-mars", "#8E3B22", "#3A140E", "#5A2419", "#7A3424", "#C2553D", "#FF9A5A", "#FFE1D2");

        private final String styleClass;
        public final Color top;
        public final Color bottom;
        public final Color panel;
        public final Color raised;
        public final Color line;
        public final Color accent;
        public final Color soft;

        World(String styleClass, String top, String bottom, String panel, String raised, String line,
                String accent, String soft) {
            this.styleClass = styleClass;
            this.top = Color.web(top);
            this.bottom = Color.web(bottom);
            this.panel = Color.web(panel);
            this.raised = Color.web(raised);
            this.line = Color.web(line);
            this.accent = Color.web(accent);
            this.soft = Color.web(soft);
        }


        public String styleClass() {
            return styleClass;
        }

        public static World of(Mission.Destination destination) {
            return switch (destination) {
                case EARTH -> EARTH;
                case MOON -> MOON;
                case MARS -> MARS;
            };
        }
    }

    public static Font body(double size) {
        return Font.font(FONT_BODY, FontWeight.NORMAL, size);
    }

    public static Font bold(double size) {
        return Font.font(FONT_BODY, FontWeight.BOLD, size);
    }

    public static Font display(double size) {
        return Font.font(FONT_DISPLAY, FontWeight.BOLD, size);
    }

    public static Color alpha(Color color, double opacity) {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
    }


    public static Map<String, Color> stylesheetTokens() {
        var tokens = new LinkedHashMap<String, Color>();
        tokens.put("-navy-950", NAVY_950);
        tokens.put("-navy-900", NAVY_900);
        tokens.put("-navy-800", NAVY_800);
        tokens.put("-navy-700", NAVY_700);
        tokens.put("-navy-600", NAVY_600);
        tokens.put("-shell-line", SHELL_LINE);
        tokens.put("-shell-line-strong", SHELL_LINE_STRONG);
        tokens.put("-text", TEXT);
        tokens.put("-text-soft", TEXT_SOFT);
        tokens.put("-cyan", CYAN);
        tokens.put("-cyan-soft", CYAN_SOFT);
        tokens.put("-orange", ORANGE);
        tokens.put("-orange-high", ORANGE_HIGH);
        tokens.put("-orange-ink", ORANGE_INK);
        tokens.put("-amber", AMBER);
        tokens.put("-alert", ALERT);
        tokens.put("-good", GOOD);
        tokens.put("-card", CARD);
        tokens.put("-card-hover", CARD_HOVER);
        tokens.put("-card-ink", CARD_INK);
        tokens.put("-card-soft", CARD_SOFT);
        tokens.put("-card-line", CARD_LINE);
        tokens.put("-card-picked", CARD_PICKED);
        tokens.put("-card-ring", CARD_RING);
        tokens.put("-chip", CHIP);
        tokens.put("-chip-ink", CHIP_INK);
        for (World world : World.values()) {
            String prefix = world == World.HANGAR ? "" : world.styleClass() + "/";
            tokens.put(prefix + "-world-top", world.top);
            tokens.put(prefix + "-world-bottom", world.bottom);
            tokens.put(prefix + "-world-panel", world.panel);
            tokens.put(prefix + "-world-raised", world.raised);
            tokens.put(prefix + "-world-line", world.line);
            tokens.put(prefix + "-world-accent", world.accent);
            tokens.put(prefix + "-world-soft", world.soft);
        }
        return tokens;
    }
}
