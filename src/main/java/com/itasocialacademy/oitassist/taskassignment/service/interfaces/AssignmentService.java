package com.itasocialacademy.oitassist.taskassignment.service.interfaces;

import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.UpdateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssignmentService {
    TaskAssignmentResponseDTO assignTask(Long tourId, CreateTaskAssignmentRequestDTO request);

    Page<TaskAssignmentResponseDTO> getAssignmentsByTourId(Pageable pageable, Long tourId);

    TaskAssignmentResponseDTO getTaskAssignmentById(Long taskAssignmentId);

    TaskAssignmentResponseDTO updateTaskAssignment(Long taskAssignmentId, UpdateTaskAssignmentRequestDTO request);
}
