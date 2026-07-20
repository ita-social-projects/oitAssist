package com.itasocialacademy.oitassist.taskassignment.dto.request;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateTaskAssignmentRequestDTO(
    @NotNull Long taskBodyId,
    AssignmentVisibility assignmentVisibility,
    @NotNull @Min(1) Integer maxPoints,
    @NotNull @Valid TaskRequirementsRequestDTO requirements
) {
}
