package com.liftoff.engine.sim;


public record LaunchIssue(Kind kind, double needed, double available, String messageKey) {

    public enum Kind {

        MASS_OVER_CAPACITY("launch.issue.massOverCapacity"),

        ROCKET_NOT_OFFERED("launch.issue.rocketNotOffered"),

        FUEL_SHORT("launch.issue.fuelShort"),

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
