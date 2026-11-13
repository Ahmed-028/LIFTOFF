package com.liftoff.render;

import javafx.scene.paint.Color;

/** The colours of the visual identity, for drawn art. Mirrors the tokens in liftoff.css. */
public final class Palette {

    public static final Color SPACE_900 = Color.web("#0F1535");
    public static final Color SPACE_500 = Color.web("#2E3A80");
    public static final Color PAPER = Color.web("#FFF8E7");
    public static final Color INK = Color.web("#1B1B3A");
    public static final Color STAR = Color.web("#FFFFFF");
    public static final Color ROCKET = Color.web("#FF6B35");
    public static final Color SKY = Color.web("#3EC1F3");

    public static final Color MASS = Color.web("#FF9F1C");
    public static final Color POWER = Color.web("#FFD23F");
    public static final Color BUDGET = Color.web("#3DDC97");
    public static final Color DATA = Color.web("#3EC1F3");
    public static final Color COMMS = Color.web("#B388FF");
    public static final Color TIME = Color.web("#FF6B9D");

    /** Outline width for art drawn on a 200-unit design grid. */
    public static final double OUTLINE = 6;

    private Palette() {
    }
}
