package com.itasocialacademy.oitassist.taskassignment.mapper;

import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskAssignment;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskRequirements;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.TaskRequirementsRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.DetailedTaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.LinkedToursResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskAssignmentMapper {
    TaskAssignment toEntity(CreateTaskAssignmentRequestDTO requestDTO);

    TaskRequirements toRequirements(TaskRequirementsRequestDTO request);

    TaskAssignmentResponseDTO toResponse(TaskAssignment entity, String taskTitle);

    TaskAssignmentDetailDTO toDetails(TaskAssignment entity);

    DetailedTaskAssignmentResponseDTO toDetailedResponse(TaskAssignment entity, String taskTitle,
        String taskDescription, List<FileDetailsDTO> files);

    @Mapping(target = "tourId", source = "id")
    LinkedToursResponseDTO toLinkedToursResponse(TourDetail tourDetail);
}
