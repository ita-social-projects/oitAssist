package com.itasocialacademy.oitassist.taskassignment.service;

import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.service.interfaces.AssignmentService;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TaskAssignmentFacadeImpl implements TaskAssignmentFacade {
    private final AssignmentService assignmentService;

    @Override
    public boolean existsByTaskBodyId(Long taskBodyId) {
        return assignmentService.existsByTaskBodyId(taskBodyId);
    }

    @Override
    public Optional<TaskAssignmentDetailDTO> findAssignmentById(Long taskAssignmentId) {
        return assignmentService.getTaskAssignmentDetailById(taskAssignmentId);
    }

    @Override
    public List<TaskAssignmentDetailDTO> findAssignmentsByTourIds(Collection<Long> tourIds) {
        return assignmentService.getTaskAssignmentDetailsByTourIds(tourIds);
    }
}