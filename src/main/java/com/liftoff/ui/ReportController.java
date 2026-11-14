package com.liftoff.ui;

import com.liftoff.engine.ResourceLevel;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.ResourceUsage;
import com.liftoff.engine.score.ScoreRules;
import com.liftoff.session.SwapAdvisor;
import com.liftoff.session.stub.Mission;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.util.List;
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
    @FXML private Label betterTitle;
    @FXML private VBox better;
    @FXML private FlowPane points;
    @FXML private javafx.scene.control.Button ending;
    @FXML private javafx.scene.control.Button nextMission;
    private com.liftoff.data.MissionEntry following;

    @Override
    protected void setUp() {
        boolean finale = session.finishedFinalMission();
        ending.setVisible(finale);
        ending.setManaged(finale);
        // The mission after this one on the main ladder, if there is one.
        var ladder = session.catalog().missions().stream().filter(com.liftoff.data.MissionEntry::ladder).toList();
        int here = ladder.indexOf(mission());
        following = here >= 0 && here + 1 < ladder.size() ? ladder.get(here + 1) : null;
        nextMission.setVisible(following != null);
        nextMission.setManaged(following != null);
        if (following != null) {
            nextMission.setText(text.get("report.nextMission", following.briefing().title()));
        }
        workedTitle.setGraphic(Icons.tick(28));
        brokeTitle.setGraphic(Icons.warning(28));
        tryTitle.setGraphic(Icons.next(28));
        betterTitle.setGraphic(Icons.forResource(ResourceType.MASS, 28));
        var rating = session.score().orElseThrow();
        var flight = session.simulation().orElseThrow();
        var report = session.report().orElseThrow();
        score.setText(text.get("report.score", "★".repeat(rating.stars()), rating.total()));
        result.setText(text.get(flight.success() ? "report.completed" : "report.incomplete"));
        com.liftoff.audio.Audio.get().play(flight.success() ? com.liftoff.audio.Sfx.WIN : com.liftoff.audio.Sfx.LOSE);
        totals.setText(text.get("report.totals", flight.totalSentGbit(), flight.totalProducedGbit(),
                flight.totalLostGbit()));
        points.getChildren().setAll(
                points("report.points.science", rating.science(), ScoreRules.SCIENCE_POINTS),
                points("report.points.margins", rating.margins(), ScoreRules.MARGIN_POINTS),
                points("report.points.budgetTime", rating.budgetTime(), ScoreRules.BUDGET_POINTS + ScoreRules.TIME_POINTS));
        addBetter(rating.science(), flight.totalSentGbit(), flight.totalProducedGbit());
        report.worked().forEach(card -> worked.getChildren().add(text.label("✓ " + describe(card), "body-text")));
        if (report.worked().isEmpty()) {
            worked.getChildren().add(text.label(text.get("report.noneWorked"), "muted"));
        }
        broke.setText(describe(report.brokeFirst()));
        next.setText(describe(report.tryNext()));
    }

    private Label points(String key, double earned, double possible) {
        var chip = text.label(text.get(key, earned, possible), "points-chip");
        chip.getStyleClass().add(earned >= possible - 1e-9 ? "full" : "partial");
        chip.setMaxWidth(Region.USE_PREF_SIZE);
        return chip;
    }

    /**
     * What could have been used better: every place the score lost points, each with the numbers
     * and the kind of change that wins the points back. It adds no rule; it reads the same
     * resource levels the score was worked out from.
     */
    private void addBetter(double science, double sentGbit, double producedGbit) {
        if (science < ScoreRules.SCIENCE_POINTS - 1e-9) {
            boolean goalMissed = session.designIssues().stream()
                    .anyMatch(issue -> issue.kind() != com.liftoff.engine.DesignIssue.Kind.ORBIT_MISSES_TARGET);
            better.getChildren().add(betterRow(Icons.data(24), text.get("report.better.science", science,
                    ScoreRules.SCIENCE_POINTS, sentGbit, producedGbit),
                    text.get(goalMissed ? "report.better.scienceGoal" : "report.better.scienceTip"), List.of()));
        }
        var resources = session.missionResources();
        for (ResourceType type : ResourceType.values()) {
            ResourceUsage usage = resources.get(type);
            if (usage.level() == ResourceLevel.OK) {
                continue;
            }
            String amount = usage.capacity().isPresent()
                    ? text.get("gauge.amountWithLimit", usage.used(), usage.capacity().getAsDouble(), type.unit())
                    : text.get("gauge.amount", usage.used(), type.unit());
            var goal = switch (type) {
                case MASS -> SwapAdvisor.Goal.MASS;
                case BUDGET -> SwapAdvisor.Goal.BUDGET;
                case POWER -> SwapAdvisor.Goal.POWER;
                default -> null;
            };
            better.getChildren().add(betterRow(Icons.forResource(type, 24),
                    text.get("report.better.resource", text.get("resource." + type), amount,
                            text.get("level." + usage.level())),
                    text.get("report.better." + type), goal == null ? List.of() : session.limitTips(goal)));
        }
        var build = session.buildReport();
        if (mission().destination() != Mission.Destination.EARTH && build.fuelLevel() != ResourceLevel.OK) {
            better.getChildren().add(betterRow(Icons.fuel(24),
                    text.get("report.better.resource", text.get("gauge.fuel"),
                            text.get("report.better.fuelAmount", build.deltaVAvailableMs(), build.deltaVRequiredMs()),
                            text.get("level." + build.fuelLevel())),
                    text.get("report.better.FUEL"), session.limitTips(SwapAdvisor.Goal.FUEL)));
        }
        if (better.getChildren().size() == 2) {
            better.getChildren().add(text.label(text.get("report.better.none"), "body-text"));
        }
    }

    private HBox betterRow(Node icon, String finding, String advice, List<SwapAdvisor.Swap> swaps) {
        var lines = new VBox(4, text.label(finding, "better-finding"), text.label(advice, "body-text"));
        for (var swap : swaps) {
            lines.getChildren().add(text.label(text.get("report.better.swap", text.get("slot." + swap.from().slot()),
                    swap.from().kidName(), swap.to().kidName()), "better-swap"));
        }
        HBox.setHgrow(lines, Priority.ALWAYS);
        var row = new HBox(12, icon, lines);
        row.getStyleClass().add("better-row");
        return row;
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
    private void onNextMission() {
        session.selectMission(following.id());
        navigator.playCutscene(com.liftoff.app.Cutscene.MISSION, () -> navigator.showRoadMap(List.of()));
    }

    @FXML
    private void onRedesign() {
        session.redesign();
        navigator.showMissionStep();
    }
}
