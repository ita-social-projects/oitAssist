package com.itasocialacademy.oitassist.competition.dao.dto.request;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionLevel;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "DTO for creating competition")
public class CreateCompetitionDto implements CreateEntityDTO<Long> {
    private String name;
    private CompetitionLevel level;
    private CompetitionStatus competitionStatus;
    private Integer year;
    private ZonedDateTime startAt;
    private ZonedDateTime entAt;
}
