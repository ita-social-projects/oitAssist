package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Builder
@Schema(description = "DTO representing an Application list rejecting response")
public record RejectedApplicationListResponse(
    ApplicationDecisionSummary application,
    List<SucceededApplicationRejectingItemResponse> succeeded,
    List<FailedApplicationDecisionItemResponse> failed) {
}
