package com.itasocialacademy.oitassist.taskassignment.service.interfaces;

import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;

public interface AssignmentService {
    TaskAssignmentResponseDTO assignTask(Long tourId, CreateTaskAssignmentRequestDTO request);
}
