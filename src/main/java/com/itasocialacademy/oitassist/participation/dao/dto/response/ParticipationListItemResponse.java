package com.itasocialacademy.oitassist.participation.dao.dto.response;

public record ParticipationListItemResponse(
    Long participationId,
    Long studentId,
    UserSummary user) {
}
