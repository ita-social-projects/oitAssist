package com.itasocialacademy.oitassist.participation.dao.dto.request;

import java.util.List;
import lombok.Builder;

@Builder
public record CreateInvitationRequest(
    Long competitionId,
    Long stageId,
    List<Long> studentIds) {
}
