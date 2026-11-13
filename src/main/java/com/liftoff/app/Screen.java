package com.liftoff.app;

/** The screens of the game and the FXML file that lays each one out. */
public enum Screen {
    TITLE("title.fxml"),
    MISSION_SELECT("mission-select.fxml"),
    BRIEFING("briefing.fxml"),
    SHIP("ship-builder.fxml"),
    SUPPLIES("supplies.fxml"),
    REVIEW("review.fxml"),
    LAUNCH("launch.fxml"),
    FLY("fly.fxml"),
    REPORT("report.fxml"),
    SOURCES("sources.fxml");

    private final String fxmlFile;

    Screen(String fxmlFile) {
        this.fxmlFile = fxmlFile;
    }

    public String fxmlFile() {
        return fxmlFile;
    }
}
