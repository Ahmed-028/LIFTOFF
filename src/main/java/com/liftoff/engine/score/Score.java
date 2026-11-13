package com.liftoff.engine.score;

/**
 * The score of one mission.
 *
 * @param total      0 to 100: the three parts added up
 * @param science    0 to 60: the share of the required data returned
 * @param margins    0 to 20: how many resources kept a healthy margin
 * @param budgetTime 0 to 20: on budget (10) and on time (10)
 * @param stars      1 to 3
 */
public record Score(double total, double science, double margins, double budgetTime, int stars) {
}
