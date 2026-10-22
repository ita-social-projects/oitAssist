package com.itasocialacademy.oitassist.participation.dao.dto.event;

import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import java.util.List;

public record InvitationRequestEvent(
    String competitionTitle,
    String stageTitle,
    List<UserProfileDetails> users) {
}
