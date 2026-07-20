package com.itasocialacademy.oitassist.taskassignment.service;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskAssignment;
import com.itasocialacademy.oitassist.taskassignment.dao.repository.TaskAssignmentRepository;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAlreadyAssignedException;
import com.itasocialacademy.oitassist.taskassignment.mapper.TaskAssignmentMapper;
import com.itasocialacademy.oitassist.taskassignment.service.interfaces.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskAssignmentMapper taskAssignmentMapper;

    @Override
    @Transactional
    public TaskAssignmentResponseDTO assignTask(Long tourId, CreateTaskAssignmentRequestDTO request) {
        // TODO: add a check for tour existence by its id

        // TODO: add a check for task existence by its id

        if (taskAssignmentRepository.existsByTaskBodyIdAndTourId(request.taskBodyId(), tourId)) {
            throw new TaskAlreadyAssignedException(request.taskBodyId(), tourId);
        }

        TaskAssignment taskAssignment = taskAssignmentMapper.toEntity(request);

        taskAssignment.setTourId(tourId);
        taskAssignment.setRequirements(taskAssignmentMapper.toRequirements(request.requirements()));

        if (request.assignmentVisibility() == null) {
            taskAssignment.setVisibility(AssignmentVisibility.HIDDEN);
        }

        log.debug("Assigned task {} to tour {}", request.taskBodyId(), tourId);

        // TODO: use real task title, when the task facade will be introduced
        return taskAssignmentMapper.toResponse(taskAssignmentRepository.save(taskAssignment), "mock title");
    }
}
