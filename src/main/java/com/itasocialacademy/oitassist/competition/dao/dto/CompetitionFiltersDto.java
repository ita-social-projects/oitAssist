package com.itasocialacademy.oitassist.competition.dao.dto;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionLevel;
import java.util.List;

public record CompetitionFiltersDto(
    List<Integer> years,
    List<CompetitionLevel> levels
) {
}
