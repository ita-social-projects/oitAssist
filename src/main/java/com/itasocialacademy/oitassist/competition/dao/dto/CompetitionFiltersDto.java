package com.itasocialacademy.oitassist.competition.dao.dto;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionLevel;
import java.util.List;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CompetitionFiltersDto {
    private List<Integer> years;
    private List<CompetitionLevel> levels;
}
