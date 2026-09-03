package com.itasocialacademy.oitassist.participation.dao.dto.response;

public record FailedApplicationDecisionItemResponse(
    Long applicationId,
    String reason) {
}
