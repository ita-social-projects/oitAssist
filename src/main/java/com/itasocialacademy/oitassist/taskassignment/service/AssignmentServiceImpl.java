package com.itasocialacademy.oitassist.taskassignment.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.task.api.TaskBodyFacade;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskAssignment;
import com.itasocialacademy.oitassist.taskassignment.dao.repository.TaskAssignmentRepository;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateAndAssignTaskRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.UpdateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.DetailedTaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.LinkedToursResponseDTO;
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
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskAssignmentMapper taskAssignmentMapper;
    private final CompetitionFacade competitionFacade;
    private final TaskBodyFacade taskBodyFacade;
    private final FileManagerFacade fileManagerFacade;
    private final SecurityFacade securityFacade;

    @Override
    @Transactional
    public DetailedTaskAssignmentResponseDTO assignTask(Long tourId, CreateTaskAssignmentRequestDTO request) {
        TourDetail tour = competitionFacade.findTourById(tourId).orElseThrow(
            () -> new TourNotFoundException(tourId));

        validateTourStatus(tour, "Cannot assign task.");

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
        List<FileDetailsDTO> files = resolveTaskFiles(taskBody.id());

        return taskAssignmentMapper.toDetailedResponse(savedTaskAssignment, taskBody.title(), taskBody.description(),
            files);
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
    public DetailedTaskAssignmentResponseDTO getTaskAssignmentById(Long taskAssignmentId) {
        // TODO: add check to see if ordinary user is in the stage participiant list
        TaskAssignment assignment = taskAssignmentRepository.findById(taskAssignmentId).orElseThrow(
            () -> new TaskAssignmentNotFoundException(taskAssignmentId));

        TaskBodyDetail taskBody = taskBodyFacade.findTaskBodyById(assignment.getTaskBodyId()).orElseThrow(
            () -> new TaskNotFoundException(assignment.getTaskBodyId()));

        List<FileDetailsDTO> files = resolveTaskFiles(taskBody.id());

        log.debug("Get Task Assignment: Id {}", assignment.getId());
        return taskAssignmentMapper.toDetailedResponse(assignment, taskBody.title(), taskBody.description(), files);
    }

    @Override
    @Transactional
    public DetailedTaskAssignmentResponseDTO updateTaskAssignment(Long taskAssignmentId,
        UpdateTaskAssignmentRequestDTO request) {
        TaskAssignment assignment = taskAssignmentRepository.findById(taskAssignmentId).orElseThrow(
            () -> new TaskAssignmentNotFoundException(taskAssignmentId));

        TourDetail tour = competitionFacade.findTourById(assignment.getTourId()).orElseThrow(
            () -> new TourNotFoundException(assignment.getTourId()));

        validateTourStatus(tour, "Cannot update task assignment.");

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

        TaskAssignment savedAssignment = taskAssignmentRepository.save(assignment);
        List<FileDetailsDTO> files = resolveTaskFiles(taskBody.id());

        log.debug("Update Task Assignment: Id {}", taskAssignmentId);
        return taskAssignmentMapper.toDetailedResponse(savedAssignment, taskBody.title(), taskBody.description(),
            files);
    }

    @Override
    @Transactional
    public void deleteTaskAssignment(Long taskAssignmentId) {
        TaskAssignment assignment = taskAssignmentRepository.findById(taskAssignmentId).orElseThrow(
            () -> new TaskAssignmentNotFoundException(taskAssignmentId));

        TourDetail tour = competitionFacade.findTourById(assignment.getTourId()).orElseThrow(
            () -> new TourNotFoundException(assignment.getTourId()));

        validateTourStatus(tour, "Cannot delete task assignment.");

        taskAssignmentRepository.delete(assignment);

        log.debug("Deleted Task Assignment: Id {}", assignment.getId());
    }

    @Override
    @Transactional
    public DetailedTaskAssignmentResponseDTO createAndAssignTask(Long tourId, CreateAndAssignTaskRequestDTO request) {
        TourDetail tour = competitionFacade.findTourById(tourId)
            .orElseThrow(() -> new TourNotFoundException(tourId));

        validateTourStatus(tour, "Cannot create task assignment.");

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

        List<FileDetailsDTO> files = resolveTaskFiles(createdTask.id());
        return taskAssignmentMapper.toDetailedResponse(saved, createdTask.title(), createdTask.description(), files);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaskAssignmentDetailDTO> getTaskAssignmentDetailById(Long taskAssignmentId) {
        return taskAssignmentRepository.findById(taskAssignmentId).map(taskAssignmentMapper::toDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTaskBodyId(Long taskBodyId) {
        return taskAssignmentRepository.existsByTaskBodyId(taskBodyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkedToursResponseDTO> getLinkedToursByTaskId(Long taskBodyId) {
        TaskBodyDetail taskBody = taskBodyFacade.findTaskBodyById(taskBodyId).orElseThrow(
            () -> new TaskNotFoundException(taskBodyId));

        checkAdminOrOrg();

        List<Long> tourIds = taskAssignmentRepository.findTourIdsByTaskBodyId(taskBody.id());

        List<TourDetail> tours =
            tourIds.stream()
                .map(id -> competitionFacade.findTourById(id).orElseThrow(() -> new TourNotFoundException(id)))
                .toList();

        return tours.stream().map(taskAssignmentMapper::toLinkedToursResponse).toList();
    }

    // helpers
    private List<FileDetailsDTO> resolveTaskFiles(Long taskBodyId) {
        Set<FileRole> allowedFileRoles;

        if (securityFacade.hasRole("ADMIN") || securityFacade.hasRole("ORG")) {
            allowedFileRoles = Set.of(FileRole.PROBLEM, FileRole.REFERENCE, FileRole.SOLUTION);
        } else {
            allowedFileRoles = Set.of(FileRole.PROBLEM, FileRole.REFERENCE);
        }

        return fileManagerFacade.getFilesByEntity(RelatedEntityType.TASK, taskBodyId, allowedFileRoles);
    }

    private void validateTourStatus(TourDetail tour, String msg) {
        if (tour.executionStatus() != ExecutionStatus.SCHEDULED) {
            throw new CompetitionHierarchyValidationException(msg + " Tour has already started");
        }
    }

    private void checkAdminOrOrg() {
        if (!securityFacade.hasRole("ADMIN") && !securityFacade.hasRole("ORG")) {
            throw new AuthorizationException("You do not have permission to this operation", ErrorCode.ACCESS_DENIED);
        }
    }
}
