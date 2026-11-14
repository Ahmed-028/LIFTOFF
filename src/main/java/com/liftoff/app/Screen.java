package com.liftoff.app;


public enum Screen {
    CUTSCENE("cutscene.fxml"),
    TITLE("title.fxml"),
    MISSION_SELECT("mission-select.fxml"),
    ROAD_MAP("road-map.fxml"),
    STAR_MATCH("star-match.fxml"),
    ASTEROID_DASH("asteroid-dash.fxml"),
    BRIEFING("briefing.fxml"),
    PLAN("flight-plan.fxml"),
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
