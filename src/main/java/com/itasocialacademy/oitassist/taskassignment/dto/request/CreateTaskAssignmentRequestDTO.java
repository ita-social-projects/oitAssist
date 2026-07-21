package com.itasocialacademy.oitassist.taskassignment.dto.request;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateTaskAssignmentRequestDTO(
    @NotNull Long taskBodyId,
    AssignmentVisibility visibility,
    @NotNull @Min(1) @Max(120) Integer maxPoints,
    @NotNull @Valid TaskRequirementsRequestDTO requirements) {
}
