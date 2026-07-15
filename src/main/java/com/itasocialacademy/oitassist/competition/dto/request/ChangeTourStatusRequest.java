package com.itasocialacademy.oitassist.competition.dto.request;

import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTourStatusRequest(
    @NotNull(message = "Execution status must not be null")
    ExecutionStatus status
) {
}