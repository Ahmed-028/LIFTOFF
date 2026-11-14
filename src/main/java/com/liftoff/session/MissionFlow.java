package com.liftoff.session;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Which mission step is current and which steps are open (table in document 05, section 3).
 * A step is open only when its own condition and those of every step before it hold. Going back
 * is always allowed; going forward to a locked step throws with the reason's message key.
 */
public final class MissionFlow {

    public static final String BRIEFING_UNREAD = "flow.locked.briefingUnread";
    public static final String SHIP_INCOMPLETE = "flow.locked.shipIncomplete";
    public static final String SUPPLIES_NOT_VISITED = "flow.locked.suppliesNotVisited";
    public static final String BUILD_NOT_CONFIRMED = "flow.locked.buildNotConfirmed";
    public static final String LAUNCH_NOT_PASSED = "flow.locked.launchNotPassed";
    public static final String SIMULATION_NOT_FINISHED = "flow.locked.simulationNotFinished";
    public static final String REDESIGN_ONLY_FROM_REPORT = "flow.redesign.onlyFromReport";

    private static final MissionStep[] STEPS = MissionStep.values();

    private final BooleanSupplier shipComplete;
    private MissionStep current = MissionStep.BRIEFING;
    private boolean briefingRead;
    private boolean suppliesVisited;
    private boolean buildConfirmed;
    private boolean launchPassed;
    private boolean simulationFinished;

    /** @param shipComplete answers whether all five module slots are filled right now */
    public MissionFlow(BooleanSupplier shipComplete) {
        this.shipComplete = Objects.requireNonNull(shipComplete);
    }

    public MissionStep current() {
        return current;
    }

    /** Moves one step forward; leaving the briefing marks it read. Stays put on the last step. */
    public MissionStep next() {
        if (current == MissionStep.BRIEFING) {
            briefingRead = true;
        }
        if (current.ordinal() == STEPS.length - 1) {
            return current;
        }
        return goTo(STEPS[current.ordinal() + 1]);
    }

    /** Moves one step back. Stays put on the first step. */
    public MissionStep back() {
        if (current.ordinal() > 0) {
            current = STEPS[current.ordinal() - 1];
        }
        return current;
    }

    /** @throws IllegalStateException with the message key of the reason, if the step is ahead and locked */
    public MissionStep goTo(MissionStep step) {
        Objects.requireNonNull(step);
        if (step.ordinal() > current.ordinal()) {
            Optional<String> reason = reasonLocked(step);
            if (reason.isPresent()) {
                throw new IllegalStateException(reason.get());
            }
        }
        current = step;
        if (step == MissionStep.SUPPLIES) {
            suppliesVisited = true;
        }
        return current;
    }

    public boolean isUnlocked(MissionStep step) {
        return reasonLocked(step).isEmpty();
    }

    /** The message key of the first unmet condition on the way to the step; empty if it is open. */
    public Optional<String> reasonLocked(MissionStep step) {
        for (MissionStep earlier : STEPS) {
            if (earlier.ordinal() > step.ordinal()) {
                break;
            }
            String reason = ownReason(earlier);
            if (reason != null) {
                return Optional.of(reason);
            }
        }
        return Optional.empty();
    }

    private String ownReason(MissionStep step) {
        return switch (step) {
            case BRIEFING -> null;
            case PLAN -> briefingRead ? null : BRIEFING_UNREAD;
            case SHIP -> null;
            case SUPPLIES -> shipComplete.getAsBoolean() ? null : SHIP_INCOMPLETE;
            case REVIEW -> suppliesVisited ? null : SUPPLIES_NOT_VISITED;
            case LAUNCH -> buildConfirmed ? null : BUILD_NOT_CONFIRMED;
            case FLY -> launchPassed ? null : LAUNCH_NOT_PASSED;
            case REPORT -> simulationFinished ? null : SIMULATION_NOT_FINISHED;
        };
    }

    /**
     * Call after any change to the design: the build is no longer confirmed and Launch onward is
     * locked again. If the current step is no longer open, falls back to the last one that is.
     */
    public void partsChanged() {
        clearResults();
        while (!isUnlocked(current)) {
            current = STEPS[current.ordinal() - 1];
        }
    }

    /** Records Confirm Build. The caller checks the build report; the flow checks the review is open. */
    public void confirmBuild() {
        require(MissionStep.REVIEW);
        buildConfirmed = true;
    }

    public void markLaunchPassed() {
        require(MissionStep.LAUNCH);
        launchPassed = true;
    }

    public void markSimulationFinished() {
        require(MissionStep.FLY);
        simulationFinished = true;
    }

    /** From the report, back to the ship step: the design stays, the results are cleared. */
    public void redesign() {
        if (current != MissionStep.REPORT) {
            throw new IllegalStateException(REDESIGN_ONLY_FROM_REPORT);
        }
        clearResults();
        current = MissionStep.SHIP;
    }

    private void clearResults() {
        buildConfirmed = false;
        launchPassed = false;
        simulationFinished = false;
    }

    private void require(MissionStep step) {
        Optional<String> reason = reasonLocked(step);
        if (reason.isPresent()) {
            throw new IllegalStateException(reason.get());
        }
    }
}
