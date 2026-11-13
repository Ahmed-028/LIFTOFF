package com.liftoff.ui;

import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.app.ScreenController;
import com.liftoff.engine.ResourceReport;
import com.liftoff.engine.ResourceType;
import com.liftoff.model.Objective;
import com.liftoff.session.GameSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

/** The mission select screen: one card per objective and a briefing for the chosen one. */
public final class MissionSelectController implements ScreenController {

    @FXML
    private HBox cardRow;
    @FXML
    private Label briefingTitle;
    @FXML
    private Label briefingBody;

    @FXML
    private void initialize() {
        // Keep the whole briefing readable: let it take the lines it needs rather than cutting it off.
        briefingBody.setMinHeight(Region.USE_PREF_SIZE);
    }

    private final Map<String, ObjectiveCard> cards = new LinkedHashMap<>();
    private final Runnable onSessionChange = this::refresh;

    private SceneNavigator navigator;
    private GameSession session;
    private ResourceBundle messages;

    @Override
    public void attach(SceneNavigator navigator, GameSession session, ResourceBundle messages) {
        this.navigator = navigator;
        this.session = session;
        this.messages = messages;

        for (Objective objective : session.catalog().objectives()) {
            ObjectiveCard card = new ObjectiveCard(objective, messages,
                    () -> session.selectObjective(objective.id()));
            HBox.setHgrow(card, Priority.ALWAYS);
            cards.put(objective.id(), card);
            cardRow.getChildren().add(card);
        }

        session.addListener(onSessionChange);
        refresh();
    }

    @Override
    public void dispose() {
        session.removeListener(onSessionChange);
    }

    @FXML
    private void onBack() {
        navigator.show(Screen.TITLE);
    }

    /** Redraws from the session: which card is chosen, and the limits the engine reports for it. */
    private void refresh() {
        Objective chosen = session.design().objective();
        cards.forEach((id, card) -> card.setChosen(chosen != null && chosen.id().equals(id)));

        if (chosen == null) {
            briefingTitle.setText(messages.getString("briefing.none"));
            briefingBody.setText("");
            return;
        }

        ResourceReport resources = session.resources();
        briefingTitle.setText(MessageFormat.format(messages.getString("briefing.title"), chosen.title()));
        briefingBody.setText(MessageFormat.format(messages.getString("briefing.body"),
                chosen.description(),
                resources.get(ResourceType.BUDGET).capacity().orElse(0),
                resources.get(ResourceType.TIME).capacity().orElse(0),
                resources.get(ResourceType.TIME).used(),
                chosen.targetLatitudeDeg(),
                chosen.basedOn()));
    }
}
