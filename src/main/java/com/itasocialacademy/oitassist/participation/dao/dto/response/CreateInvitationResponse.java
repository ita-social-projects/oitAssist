package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record CreateInvitationResponse(
    Long id,
    Long competitionId,
    Long stageId,
    List<Long> studentIds,
    Long issuedBy,
    Instant issuedAt,
    RequestStatus status) {
}
