package com.itasocialacademy.oitassist.taskassignment.mapper;

import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskAssignment;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskRequirements;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.TaskRequirementsRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskAssignmentMapper {
    TaskAssignment toEntity(CreateTaskAssignmentRequestDTO requestDTO);

    TaskRequirements toRequirements(TaskRequirementsRequestDTO request);

    TaskAssignmentResponseDTO toResponse(TaskAssignment entity, String taskTitle);
}
