package com.itasocialacademy.oitassist.participation.dao.dto.event;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;

public record ApplicationDecisionEvent(
    String competitionTitle,
    String stageTitle,
    String firstName,
    String email,
    String rejectionReason,
    RequestStatus status) {
}
