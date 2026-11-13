package com.liftoff;

import com.liftoff.app.LiftoffApp;
import javafx.application.Application;

/** Plain entry point, so the game can also be started from a jar or an IDE without JavaFX launcher settings. */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(LiftoffApp.class, args);
    }
}
