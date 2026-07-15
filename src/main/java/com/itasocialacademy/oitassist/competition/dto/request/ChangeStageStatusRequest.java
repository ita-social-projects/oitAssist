package com.itasocialacademy.oitassist.competition.dto.request;

import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStageStatusRequest(
    @NotNull(message = "Stage status must not be null")
    StageStatus status
) {
}
