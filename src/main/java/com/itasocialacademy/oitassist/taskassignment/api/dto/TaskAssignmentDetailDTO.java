package com.itasocialacademy.oitassist.taskassignment.api.dto;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;

/**
 * DTO representing the task assignment entity for cross-module communication.
 */
public record TaskAssignmentDetailDTO(
    Long id,
    Long taskBodyId,
    Long tourId,
    AssignmentVisibility visibility,
    Integer maxPoints,
    TaskRequirementsDTO requirements) {
}
