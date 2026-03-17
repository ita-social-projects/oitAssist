package com.itasocialacademy.oitassist.competition.dao.dto;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Schema(description = "Dto representing list of years and levels in the Competitions")
public class CompetitionFiltersDto {
    @Schema(description = "list of the years")
    private List<Integer> years;
    @Schema(description = "list of the levels")
    private List<CompetitionLevel> levels;
}
