package com.itasocialacademy.oitassist.participation.dao.dto.event;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import java.util.List;

public record ApplicationDecisionListEvent(
    String competitionTitle,
    String stageTitle,
    List<UserProfileDetails> users,
    String rejectionReason,
    RequestStatus status) {
}
