package com.itasocialacademy.oitassist.competition.dao.dto.response;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionLevel;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
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
@Schema(description = "DTO representing competition response")
public class ResponseCompetitionDto implements EntityDTO<Long> {
    private Long id;
    private String name;
    private CompetitionLevel level;
    private CompetitionStatus competitionStatus;
    private Integer year;
    private ZonedDateTime startAt;
    private ZonedDateTime entAt;
}
