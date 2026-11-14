package com.liftoff.app;

import com.liftoff.session.GameSession;

import java.util.ResourceBundle;


public interface ScreenController {


    void attach(SceneNavigator navigator, GameSession session, ResourceBundle messages);


    default void dispose() {
    }
}
