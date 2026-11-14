package com.liftoff;

import com.liftoff.app.LiftoffApp;
import javafx.application.Application;


public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(LiftoffApp.class, args);
    }
}
