package com.itasocialacademy.oitassist.taskassignment.dto.response;

import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskRequirements;
import java.util.List;

public record DetailedTaskAssignmentResponseDTO(
    Long id,
    Long taskBodyId,
    String taskTitle,
    String taskDescription,
    Long tourId,
    AssignmentVisibility visibility,
    Integer maxPoints,
    TaskRequirements requirements,
    List<FileDetailsDTO> files,
    Long createdBy) {
}
