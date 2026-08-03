package com.itasocialacademy.oitassist.participation.dao.dto.event;

public record ApplicationAcceptedEvent(
    String competitionTitle,
    String stageTitle,
    String firstName,
    String email
) {
}
