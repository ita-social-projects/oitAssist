package com.itasocialacademy.oitassist.participation.dao.dto.event;

import lombok.Builder;

@Builder
public record CompetitionValidatedDataEvent(
    String competitionTitle,
    String stageTitle) {
}
