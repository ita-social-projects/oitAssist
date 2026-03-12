package com.itasocialacademy.oitassist.competition.dao.dto;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionLevel;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;

public record CompetitionFilter(
    String search,
    CompetitionLevel level,
    CompetitionStatus status,
    Integer year) {

}
