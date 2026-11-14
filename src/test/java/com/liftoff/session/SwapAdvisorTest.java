package com.liftoff.session;

import com.liftoff.session.SwapAdvisor.Goal;
import com.liftoff.session.stub.BuildIssue;
import com.liftoff.session.stub.BuildReport;
import com.liftoff.session.stub.ModuleSlot;
import com.liftoff.session.stub.ShipBuild;
import com.liftoff.session.stub.ShipPart;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SwapAdvisorTest {

    private static ShipPart part(String id, ModuleSlot slot, double mass, double cost, double supply, double draw) {
        return new ShipPart(id, slot, id, id, "test fake", "nothing real", List.of(),
                mass, supply, draw, 0, 0, 0, 0, 0, 0, 0, 0, 0, cost, null, id, null);
    }


    private static BuildReport calculate(ShipBuild build) {
        double mass = 0;
        double cost = 0;
        double supply = 0;
        double draw = 0;
        for (var part : build.parts().values()) {
            mass += part.massKg();
            cost += part.costMusd();
            supply += part.powerSupplyW();
            draw += part.powerDrawW();
        }
        var issues = new ArrayList<BuildIssue>();
        if (cost > 100) {
            issues.add(new BuildIssue(BuildIssue.Kind.OVER_BUDGET, null, cost, 100));
        }
        if (draw > supply) {
            issues.add(new BuildIssue(BuildIssue.Kind.POWER_SHORT, ModuleSlot.POWER, draw, supply));
        }
        return new BuildReport(mass, 0, mass, cost, supply, draw, 0, 0, issues);
    }

    private final ShipPart bigBody = part("big-body", ModuleSlot.AIRFRAME, 500, 80, 0, 10);
    private final ShipPart smallBody = part("small-body", ModuleSlot.AIRFRAME, 200, 30, 0, 10);
    private final ShipPart midBody = part("mid-body", ModuleSlot.AIRFRAME, 300, 60, 0, 10);
    private final ShipPart weakPower = part("weak-power", ModuleSlot.POWER, 50, 30, 20, 0);
    private final ShipPart cheapNoPower = part("cheap-no-power", ModuleSlot.POWER, 10, 5, 0, 0);
    private final ShipPart strongPower = part("strong-power", ModuleSlot.POWER, 90, 50, 60, 0);

    private final ShipBuild build = ShipBuild.empty().with(bigBody).with(weakPower);
    private final Map<ModuleSlot, List<ShipPart>> options = Map.of(
            ModuleSlot.AIRFRAME, List.of(bigBody, smallBody, midBody),
            ModuleSlot.POWER, List.of(weakPower, cheapNoPower, strongPower));

    @Test
    void nothingToSuggestWhenWithinTheLimit() {
        assertTrue(SwapAdvisor.suggest(Goal.BUDGET, build, options, SwapAdvisorTest::calculate, 0, 200, 3).isEmpty());
    }

    @Test
    void swapsThatFixTheBudgetComeFirstAndOnePerSlot() {
        var tips = SwapAdvisor.suggest(Goal.BUDGET, build, options, SwapAdvisorTest::calculate, 0, 100, 3);
        assertEquals(2, tips.size(), "one tip for each slot that can help");
        var first = tips.getFirst();
        assertEquals("small-body", first.to().id(), "the cheapest body saves the most and fixes it");
        assertTrue(first.fixes());
        assertEquals(50, first.gain(), 1e-9);
    }

    @Test
    void aSwapThatCausesANewWarningRanksBelowOneThatDoesNot() {


        var tips = SwapAdvisor.suggest(Goal.BUDGET, build, options, SwapAdvisorTest::calculate, 0, 105, 3);
        assertEquals("small-body", tips.get(0).to().id());
        assertTrue(tips.get(1).newProblems().contains(BuildIssue.Kind.POWER_SHORT));
    }

    @Test
    void powerTipsLookForMoreSupply() {
        var hungry = ShipBuild.empty().with(part("hungry-body", ModuleSlot.AIRFRAME, 100, 10, 0, 40)).with(weakPower);
        var tips = SwapAdvisor.suggest(Goal.POWER, hungry, options, SwapAdvisorTest::calculate, 0, 0, 3);
        assertEquals("strong-power", tips.getFirst().to().id());
        assertTrue(tips.getFirst().fixes());
        assertFalse(tips.stream().anyMatch(tip -> tip.to().id().equals("cheap-no-power")), "a swap that makes it worse is no tip");
    }

    @Test
    void massCountsWhatIsOutsideTheShipToo() {

        assertEquals(50, SwapAdvisor.overBy(Goal.MASS, calculate(build), 100, 600), 1e-9);
        var tips = SwapAdvisor.suggest(Goal.MASS, build, options, SwapAdvisorTest::calculate, 100, 600, 1);
        assertEquals(1, tips.size());
        assertEquals("small-body", tips.getFirst().to().id());
    }
}
