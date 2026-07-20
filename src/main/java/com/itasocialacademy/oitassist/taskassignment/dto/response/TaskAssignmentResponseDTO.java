package com.itasocialacademy.oitassist.taskassignment.dto.response;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskRequirements;

public record TaskAssignmentResponseDTO(
    Long id,
    Long taskBodyId,
    String taskTitle,
    Long tourId,
    AssignmentVisibility visibility,
    Integer maxPoints,
    TaskRequirements requirements,
    Long createdBy
) {
}
