package com.itasocialacademy.oitassist.competitionhistory.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competitionhistory.dto.response.CompetitionHistoryResponse;
import com.itasocialacademy.oitassist.evaluation.api.dto.CompetitionScore;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CompetitionHistoryMapperTest {

    private final CompetitionHistoryMapper mapper = Mappers.getMapper(CompetitionHistoryMapper.class);

    @Test
    void toResponse_detailAndScore_shouldMapCompetitionFieldsAndScore() {
        ZonedDateTime dateStart = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneId.of("UTC"));
        CompetitionDetail detail = CompetitionDetail.builder()
            .id(10L)
            .title("Olympiad")
            .dateStart(dateStart)
            .dateFinish(dateStart.plusDays(10))
            .build();
        CompetitionHistoryResponse expected = CompetitionHistoryResponse.builder()
            .id(10L)
            .title("Olympiad")
            .dateStart(dateStart)
            .dateFinish(dateStart.plusDays(10))
            .score(28.5)
            .maxScore(45)
            .build();

        CompetitionHistoryResponse result = mapper.toResponse(detail, new CompetitionScore(28.5, 45));

        assertEquals(expected, result);
    }
}