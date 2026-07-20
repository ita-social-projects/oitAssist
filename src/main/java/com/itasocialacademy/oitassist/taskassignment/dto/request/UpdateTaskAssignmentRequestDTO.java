package com.itasocialacademy.oitassist.taskassignment.dto.request;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

public record UpdateTaskAssignmentRequestDTO(
    AssignmentVisibility visibility,
    @Min(1) Integer maxPoints,
    @Valid TaskRequirementsRequestDTO requirements) {
}
