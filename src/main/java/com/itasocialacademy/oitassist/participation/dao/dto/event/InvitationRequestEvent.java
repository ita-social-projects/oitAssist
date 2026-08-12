package com.itasocialacademy.oitassist.participation.dao.dto.event;

public record InvitationRequestEvent(
    String competitionTitle,
    String stageTitle,
    String firstName,
    String email) {
}
