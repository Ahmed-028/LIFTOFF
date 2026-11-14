package com.liftoff.session;

import com.liftoff.session.stub.BuildIssue;
import com.liftoff.session.stub.BuildReport;
import com.liftoff.session.stub.ModuleSlot;
import com.liftoff.session.stub.ShipBuild;
import com.liftoff.session.stub.ShipPart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public final class SwapAdvisor {


    public enum Goal { MASS, BUDGET, POWER, FUEL }


    public record Swap(ShipPart from, ShipPart to, double gain, boolean fixes, List<BuildIssue.Kind> newProblems) {
        public Swap {
            newProblems = List.copyOf(newProblems);
        }
    }

    private SwapAdvisor() {
    }


    public static double overBy(Goal goal, BuildReport report, double offset, double limit) {
        return switch (goal) {
            case MASS -> report.totalMassKg() + offset - limit;
            case BUDGET -> report.costMusd() + offset - limit;
            case POWER -> report.powerDrawW() - report.powerSupplyW();
            case FUEL -> report.deltaVRequiredMs() - report.deltaVAvailableMs();
        };
    }


    public static List<Swap> suggest(Goal goal, ShipBuild build, Map<ModuleSlot, List<ShipPart>> options,
            Function<ShipBuild, BuildReport> calculate, double offset, double limit, int max) {
        BuildReport now = calculate.apply(build);
        double before = overBy(goal, now, offset, limit);
        if (before <= 0) {
            return List.of();
        }
        var existing = kinds(now);
        var best = new ArrayList<Swap>();
        for (var slot : ModuleSlot.values()) {
            var current = build.part(slot).orElse(null);
            if (current == null) {
                continue;
            }
            Swap bestHere = null;
            for (var candidate : options.getOrDefault(slot, List.of())) {
                if (candidate.id().equals(current.id())) {
                    continue;
                }
                BuildReport after = calculate.apply(build.with(candidate));
                double remaining = overBy(goal, after, offset, limit);
                double gain = before - remaining;
                if (gain <= 1e-9) {
                    continue;
                }
                var added = kinds(after);
                added.removeAll(existing);
                added.remove(goalKind(goal));
                var swap = new Swap(current, candidate, gain, remaining <= 0, List.copyOf(added));
                if (bestHere == null || RANKING.compare(swap, bestHere) < 0) {
                    bestHere = swap;
                }
            }
            if (bestHere != null) {
                best.add(bestHere);
            }
        }
        best.sort(RANKING);
        return List.copyOf(best.subList(0, Math.min(max, best.size())));
    }


    private static final Comparator<Swap> RANKING = Comparator
            .comparing((Swap swap) -> !swap.fixes())
            .thenComparingInt(swap -> swap.newProblems().size())
            .thenComparing(Comparator.comparingDouble(Swap::gain).reversed());

    private static EnumSet<BuildIssue.Kind> kinds(BuildReport report) {
        var kinds = EnumSet.noneOf(BuildIssue.Kind.class);
        report.issues().forEach(issue -> kinds.add(issue.kind()));
        return kinds;
    }

    private static BuildIssue.Kind goalKind(Goal goal) {
        return switch (goal) {
            case MASS -> BuildIssue.Kind.PAYLOAD_TOO_HEAVY;
            case BUDGET -> BuildIssue.Kind.OVER_BUDGET;
            case POWER -> BuildIssue.Kind.POWER_SHORT;
            case FUEL -> BuildIssue.Kind.FUEL_SHORT;
        };
    }
}
