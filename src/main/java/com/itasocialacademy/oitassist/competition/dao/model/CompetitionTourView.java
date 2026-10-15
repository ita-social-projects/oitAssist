package com.itasocialacademy.oitassist.competition.dao.model;

/**
 * Projection of a tour ID together with the ID of the competition it belongs
 * to.
 */
public interface CompetitionTourView {
    Long getCompetitionId();

    Long getTourId();
}