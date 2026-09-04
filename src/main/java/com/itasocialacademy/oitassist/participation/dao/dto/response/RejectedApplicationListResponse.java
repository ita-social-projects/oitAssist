package com.itasocialacademy.oitassist.participation.dao.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record RejectedApplicationListResponse(
    ApplicationDecisionSummary application,
    List<SucceededApplicationRejectingItemResponse> succeeded,
    List<FailedApplicationDecisionItemResponse> failed) {
}
