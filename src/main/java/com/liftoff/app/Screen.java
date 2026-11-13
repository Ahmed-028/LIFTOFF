package com.liftoff.app;

/** The screens of the game and the FXML file that lays each one out. */
public enum Screen {
    TITLE("title.fxml"),
    MISSION_SELECT("mission-select.fxml");

    private final String fxmlFile;

    Screen(String fxmlFile) {
        this.fxmlFile = fxmlFile;
    }

    public String fxmlFile() {
        return fxmlFile;
    }
}
