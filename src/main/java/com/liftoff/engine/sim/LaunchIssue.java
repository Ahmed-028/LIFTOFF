package com.liftoff.engine.sim;

/**
 * One reason the launch check stops a mission.
 *
 * @param needed     what the design asks for (kg, m/s, degrees, $M or months, by kind)
 * @param available  what there is, in the same unit
 * @param messageKey key of the interface text that explains it
 */
public record LaunchIssue(Kind kind, double needed, double available, String messageKey) {

    public enum Kind {
        /** Total mass, propellant included, is more than the rocket lifts to this class of orbit. */
        MASS_OVER_CAPACITY("launch.issue.massOverCapacity"),
        /** No rocket chosen, or no cited capacity for this destination: never guessed. */
        ROCKET_NOT_OFFERED("launch.issue.rocketNotOffered"),
        /** The ship's delta-v is less than the trip needs. */
        FUEL_SHORT("launch.issue.fuelShort"),
        /** The orbit never flies over the target latitude (Earth missions only). */
        ORBIT_CANNOT_REACH_TARGET("launch.issue.orbitCannotReachTarget"),
        OVER_BUDGET("launch.issue.overBudget"),
        OVER_DEADLINE("launch.issue.overDeadline");

        private final String messageKey;

        Kind(String messageKey) {
            this.messageKey = messageKey;
        }

        public String messageKey() {
            return messageKey;
        }
    }

    static LaunchIssue of(Kind kind, double needed, double available) {
        return new LaunchIssue(kind, needed, available, kind.messageKey());
    }
}
