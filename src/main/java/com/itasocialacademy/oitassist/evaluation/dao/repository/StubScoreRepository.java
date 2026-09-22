package com.itasocialacademy.oitassist.evaluation.dao.repository;

import com.itasocialacademy.oitassist.evaluation.dao.dto.request.ParticipantTaskScore;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

/**
 * Temporary stub until real evaluation scoring exists. Parameters are part of
 * the future contract and are intentionally ignored for now.
 */
@Repository
public class StubScoreRepository {
    private static final String IHOR = "Мельник Ігор Васильович";
    private static final String KARINA = "Чорновіл Каріна Олегівна";

    /**
     * Returns a fixed stub list of participant task scores.
     *
     * @param competitionId currently unused; will be used to filter scores once real evaluation scoring is implemented
     * @param tourIds currently unused; will be used to filter scores once real evaluation scoring is implemented
     * @return a fixed stub list of scores
     */
    public List<ParticipantTaskScore> findScores(Long competitionId, Set<Long> tourIds) {
        return List.of(
            new ParticipantTaskScore(1L, IHOR, 1L, 10L, 5),
            new ParticipantTaskScore(1L, IHOR, 1L, 11L, 3),
            new ParticipantTaskScore(1L, IHOR, 2L, 12L, 7),
            new ParticipantTaskScore(2L, KARINA, 1L, 10L, 4));
    }
}
