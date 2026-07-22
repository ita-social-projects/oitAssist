package com.itasocialacademy.oitassist.taskassignment.dto.request;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateAndAssignTaskRequestDTO(
    @NotBlank String title,
    @NotBlank String description,
    @NotEmpty List<Long> fileIds,
    AssignmentVisibility visibility,
    @NotNull Integer maxPoints,
    @NotNull @Valid TaskRequirementsRequestDTO requirements) {
}
