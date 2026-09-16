package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Builder
@Schema(description = "DTO representing an Application list accepting response")
public record AcceptedApplicationListResponse(
    ApplicationDecisionSummary application,
    List<SucceededApplicationAcceptingItemResponse> succeeded,
    List<FailedApplicationDecisionItemResponse> failed) {
}
