package com.itasocialacademy.oitassist.taskassignment.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.task.api.TaskBodyFacade;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskAssignment;
import com.itasocialacademy.oitassist.taskassignment.dao.repository.TaskAssignmentRepository;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateAndAssignTaskRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.UpdateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAlreadyAssignedException;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import com.itasocialacademy.oitassist.taskassignment.mapper.TaskAssignmentMapper;
import com.itasocialacademy.oitassist.taskassignment.service.interfaces.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskAssignmentMapper taskAssignmentMapper;
    private final CompetitionFacade competitionFacade;
    private final TaskBodyFacade taskBodyFacade;

    @Override
    @Transactional
    public TaskAssignmentResponseDTO assignTask(Long tourId, CreateTaskAssignmentRequestDTO request) {
        TourDetail tour = competitionFacade.findTourById(tourId).orElseThrow(
            () -> new TourNotFoundException(tourId));

        TaskBodyDetail taskBody = taskBodyFacade.findTaskBodyById(request.taskBodyId()).orElseThrow(
            () -> new TaskNotFoundException(request.taskBodyId()));

        if (taskAssignmentRepository.existsByTaskBodyIdAndTourId(taskBody.id(), tour.id())) {
            throw new TaskAlreadyAssignedException(taskBody.id(), tour.id());
        }

        TaskAssignment taskAssignment = taskAssignmentMapper.toEntity(request);

        taskAssignment.setTourId(tour.id());

        if (request.visibility() == null) {
            taskAssignment.setVisibility(AssignmentVisibility.HIDDEN);
        }

        TaskAssignment savedTaskAssignment = taskAssignmentRepository.save(taskAssignment);

        log.debug("Assigned task {} to tour {}", taskBody.id(), tour.id());

        return taskAssignmentMapper.toResponse(savedTaskAssignment, taskBody.title());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskAssignmentResponseDTO> getAssignmentsByTourId(Pageable pageable, Long tourId) {
        TourDetail tour = competitionFacade.findTourById(tourId).orElseThrow(
            () -> new TourNotFoundException(tourId));

        log.debug("Getting all task assignments for tour {}, page={}, size={}, sort={}", tour.id(),
            pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<TaskAssignment> assignmentsPage = taskAssignmentRepository.findAllByTourId(tourId, pageable);

        if (assignmentsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> taskIds = assignmentsPage.getContent().stream()
            .map(TaskAssignment::getTaskBodyId)
            .distinct()
            .toList();

        Map<Long, String> taskTitles = taskBodyFacade.getTaskTitlesByIds(taskIds);

        return assignmentsPage.map(entity -> {
            String title = taskTitles.getOrDefault(entity.getTaskBodyId(), "Unknown Title");
            return taskAssignmentMapper.toResponse(entity, title);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAssignmentResponseDTO getTaskAssignmentById(Long taskAssignmentId) {
        TaskAssignment assignment = taskAssignmentRepository.findById(taskAssignmentId).orElseThrow(
            () -> new TaskAssignmentNotFoundException(taskAssignmentId));

        TaskBodyDetail taskBody = taskBodyFacade.findTaskBodyById(assignment.getTaskBodyId()).orElseThrow(
            () -> new TaskNotFoundException(assignment.getTaskBodyId()));

        log.debug("Get Task Assignment: Id {}", assignment.getId());
        return taskAssignmentMapper.toResponse(assignment, taskBody.title());
    }

    @Override
    @Transactional
    public TaskAssignmentResponseDTO updateTaskAssignment(Long taskAssignmentId,
        UpdateTaskAssignmentRequestDTO request) {
        TaskAssignment assignment = taskAssignmentRepository.findById(taskAssignmentId).orElseThrow(
            () -> new TaskAssignmentNotFoundException(taskAssignmentId));

        if (request.visibility() != null) {
            assignment.setVisibility(request.visibility());
        }
        if (request.maxPoints() != null) {
            assignment.setMaxPoints(request.maxPoints());
        }
        if (request.requirements() != null) {
            assignment.setRequirements(taskAssignmentMapper.toRequirements(request.requirements()));
        }

        TaskBodyDetail taskBody = taskBodyFacade.findTaskBodyById(assignment.getTaskBodyId()).orElseThrow(
            () -> new TaskNotFoundException(assignment.getTaskBodyId()));

        log.debug("Update Task Assignment: Id {}", taskAssignmentId);

        return taskAssignmentMapper.toResponse(taskAssignmentRepository.save(assignment), taskBody.title());
    }

    @Override
    @Transactional
    public void deleteTaskAssignment(Long taskAssignmentId) {
        TaskAssignment assignment = taskAssignmentRepository.findById(taskAssignmentId).orElseThrow(
            () -> new TaskAssignmentNotFoundException(taskAssignmentId));

        taskAssignmentRepository.delete(assignment);

        log.debug("Deleted Task Assignment: Id {}", assignment.getId());
    }

    @Override
    @Transactional
    public TaskAssignmentResponseDTO createAndAssignTask(Long tourId, CreateAndAssignTaskRequestDTO request) {
        TourDetail tour = competitionFacade.findTourById(tourId)
            .orElseThrow(() -> new TourNotFoundException(tourId));

        TaskBodyDetail createdTask = taskBodyFacade.createTask(
            request.title(), request.description(), request.fileIds());

        TaskAssignment taskAssignment = TaskAssignment.builder()
            .taskBodyId(createdTask.id())
            .tourId(tour.id())
            .visibility(request.visibility() != null ? request.visibility() : AssignmentVisibility.HIDDEN)
            .maxPoints(request.maxPoints())
            .requirements(taskAssignmentMapper.toRequirements(request.requirements()))
            .build();

        TaskAssignment saved = taskAssignmentRepository.save(taskAssignment);

        log.debug("Created task {} and assigned to tour {}", createdTask.id(), tour.id());

        return taskAssignmentMapper.toResponse(saved, createdTask.title());
    }
}
