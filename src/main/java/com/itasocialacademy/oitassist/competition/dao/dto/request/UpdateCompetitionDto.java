package com.itasocialacademy.oitassist.competition.dao.dto.request;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionLevel;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Schema(description = "DTO for updating Competition")
public class UpdateCompetitionDto implements UpdateEntityDTO<Long> {
    @Schema(description = "Unique identifier of the competition", example = "1")
    private Long id;
    @Schema(description = "title of the competition", example = "best Competition of the Year")
    private String name;
    @Schema(description = "level of the competition", examples = {"CITY", "REGION", "NATIONAL", "OPEN"})
    private CompetitionLevel level;
    @Schema(description = "status of the competition", examples = {"INCOMING", "INPROGRESS", "FINISED", "ARCHIVED"})
    private CompetitionStatus competitionStatus;
    @Schema(description = "year of the competition", example = "2026")
    private Integer year;
    @Schema(description = "start time of the competition", example = "2026-01-20 09:00:00.000000 +00:00")
    private ZonedDateTime startAt;
    @Schema(description = "end time of the competition", example = "2026-02-20 18:00:00.000000 +00:00")
    private ZonedDateTime entAt;
}
