package com.itasocialacademy.oitassist.evaluation.api.dto;

/**
 * Result of a user in a single competition.
 *
 * @param score    the sum of the jury scores the user received in the
 *                 competition; {@code 0.0} if none of the user's submissions
 *                 has been evaluated
 * @param maxScore the sum of the maximum points of all tasks in the competition
 */
public record CompetitionScore(
    Double score,
    Integer maxScore) {
}