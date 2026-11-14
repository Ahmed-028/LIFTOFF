package com.liftoff.ui;

import com.liftoff.engine.score.WhyCard;
import com.liftoff.render.Icons;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class ReportController extends MissionScreen {
    @FXML private Label score;
    @FXML private Label result;
    @FXML private Label totals;
    @FXML private VBox worked;
    @FXML private Label broke;
    @FXML private Label next;
    @FXML private Label workedTitle;
    @FXML private Label brokeTitle;
    @FXML private Label tryTitle;
    @FXML private javafx.scene.control.Button ending;

    @Override
    protected void setUp() {
        boolean finale = session.finishedFinalMission();
        ending.setVisible(finale);
        ending.setManaged(finale);
        workedTitle.setGraphic(Icons.tick(28));
        brokeTitle.setGraphic(Icons.warning(28));
        tryTitle.setGraphic(Icons.next(28));
        var rating = session.score().orElseThrow();
        var flight = session.simulation().orElseThrow();
        var report = session.report().orElseThrow();
        score.setText(text.get("report.score", "★".repeat(rating.stars()), rating.total()));
        result.setText(text.get(flight.success() ? "report.completed" : "report.incomplete"));
        com.liftoff.audio.Audio.get().play(flight.success() ? com.liftoff.audio.Sfx.WIN : com.liftoff.audio.Sfx.LOSE);
        totals.setText(text.get("report.totals", flight.totalSentGbit(), flight.totalProducedGbit(),
                flight.totalLostGbit(), rating.science(), rating.margins(), rating.budgetTime()));
        report.worked().forEach(card -> worked.getChildren().add(text.label("✓ " + describe(card), "body-text")));
        if (report.worked().isEmpty()) {
            worked.getChildren().add(text.label(text.get("report.noneWorked"), "muted"));
        }
        broke.setText(describe(report.brokeFirst()));
        next.setText(describe(report.tryNext()));
    }

    private String describe(WhyCard card) {
        Object[] parameters = card.params().stream().map(this::localize).toArray();
        return text.bundle().containsKey(card.messageKey()) ? text.get(card.messageKey(), parameters) : card.defaultText();
    }

    private Object localize(Object value) {
        if (value instanceof String name) {
            for (String prefix : new String[] { "measurement.", "resource.", "duration.single." }) {
                if (text.bundle().containsKey(prefix + name)) {
                    return text.get(prefix + name);
                }
            }
        }
        return value;
    }

    @FXML
    private void onEnding() {
        navigator.playCutscene(com.liftoff.app.Cutscene.ENDING, () -> navigator.show(com.liftoff.app.Screen.TITLE));
    }

    @FXML
    private void onRedesign() {
        session.redesign();
        navigator.showMissionStep();
    }
}
