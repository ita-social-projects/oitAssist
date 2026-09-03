package com.itasocialacademy.oitassist.participation.dao.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record AcceptedApplicationListResponse(
    List<SucceededApplicationAcceptingItemResponse> succeededApplications,
    List<FailedApplicationDecisionItemResponse> failedApplications) {
}
