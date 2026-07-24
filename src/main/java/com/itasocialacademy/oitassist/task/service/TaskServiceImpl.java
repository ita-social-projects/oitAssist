package com.itasocialacademy.oitassist.task.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.api.events.FilesAttachRequestedEvent;
import com.itasocialacademy.oitassist.filemanager.api.events.FilesDetachRequestedEvent;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.task.api.events.TaskDeletionRequestEvent;
import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dao.model.TaskTitleView;
import com.itasocialacademy.oitassist.task.dao.repository.TaskBodyRepository;
import com.itasocialacademy.oitassist.task.dto.request.ChangeOwnerRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.UpdateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import com.itasocialacademy.oitassist.task.exceptions.TaskAccessRestrictedException;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.task.mapper.TaskBodyMapper;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {
    private final TaskBodyRepository taskBodyRepository;
    private final TaskBodyMapper taskBodyMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SecurityFacade securityFacade;
    private final UserFacade userFacade;

    @Override
    @Transactional
    public TaskResponseDTO createTask(CreateTaskRequestDTO requestDTO) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to create tasks",
                ErrorCode.ACCESS_DENIED));

        TaskBody task = taskBodyMapper.toEntity(requestDTO);
        task.setOwnerId(currentUserId);

        TaskBody createdTask = taskBodyRepository.save(task);

        log.debug("Created Task: Id {}; Title - {}", createdTask.getId(), createdTask.getTitle());
        publishAttachEvent(createdTask.getId(), requestDTO.fileIds(), createdTask.getCreatedBy());

        return taskBodyMapper.toResponse(createdTask);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDTO getTaskById(Long id) {
        TaskBody taskBody = taskBodyRepository.findById(id)
            .orElseThrow(
                () -> new TaskNotFoundException(id));

        log.debug("Get Task: Id {}", taskBody.getId());
        return taskBodyMapper.toResponse(taskBody);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponseDTO> getAllTasks(Pageable pageable) {
        log.debug("getAllTasks: page={}, size={}, sort={}",
            pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return taskBodyRepository.findAll(pageable).map(taskBodyMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponseDTO> getAllMyTasks(Pageable pageable) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to view created tasks",
                ErrorCode.ACCESS_DENIED));
        log.debug("getAllMyTasks: userId={}, page={}, size={}, sort={}",
            currentUserId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return taskBodyRepository.findAllByOwnerId(currentUserId, pageable);
    }

    @Override
    @Transactional
    public TaskResponseDTO updateTask(Long taskId, UpdateTaskRequestDTO requestDTO) {
        TaskBody existingTask = taskBodyRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        checkOwnerOrAdmin(existingTask.getOwnerId(), existingTask.getId());

        existingTask.setTitle(requestDTO.title());
        existingTask.setDescription(requestDTO.description());

        TaskBody updatedTask = taskBodyRepository.save(existingTask);
        log.debug("Updated Task: Id {}, Title - {}", updatedTask.getId(), updatedTask.getTitle());

        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to view created tasks",
                ErrorCode.ACCESS_DENIED));

        publishAttachEvent(updatedTask.getId(), requestDTO.fileIds(), currentUserId);
        publishDetachEvent(updatedTask.getId(), requestDTO.removedFileIds(), currentUserId);

        return taskBodyMapper.toResponse(updatedTask);
    }

    @Override
    @Transactional
    public TaskResponseDTO changeTaskOwner(Long taskId, ChangeOwnerRequestDTO changeOwnerRequest) {
        if (!securityFacade.hasRole("ADMIN")) {
            throw new TaskAccessRestrictedException(taskId);
        }

        TaskBody task = taskBodyRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        UserAuthDetails userDetails = userFacade.findByEmail(changeOwnerRequest.newOwnerEmail())
            .orElseThrow(UserNotFoundException::new);

        if (!isOrgOrAdmin(userDetails)) {
            throw new ValidationException("Provided user is not ADMIN nor ORG", ErrorCode.COMMON_VALIDATION_FAILED);
        }

        if (task.getOwnerId().equals(userDetails.id())) {
            return taskBodyMapper.toResponse(task);
        }

        task.setOwnerId(userDetails.id());
        log.debug("Task {} owner changed to user {}", task.getId(), userDetails.id());

        return taskBodyMapper.toResponse(taskBodyRepository.save(task));
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        checkForAssignments(taskId);

        TaskBody taskToDelete = taskBodyRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        checkOwnerOrAdmin(taskToDelete.getOwnerId(), taskToDelete.getId());

        taskBodyRepository.delete(taskToDelete);
        log.debug("Task {} with title {} deleted", taskId, taskToDelete.getTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaskBodyDetail> getTaskBodyDetailById(Long taskId) {
        return taskBodyRepository.findById(taskId)
            .map(taskBodyMapper::toTaskBodyDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> getTaskTitlesByIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return taskBodyRepository.findTitlesByIds(taskIds).stream()
            .collect(Collectors.toMap(
                TaskTitleView::getId,
                TaskTitleView::getTitle));
    }

    // helpers
    private void publishAttachEvent(Long taskBodyId, List<Long> fileIds, Long authorId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        applicationEventPublisher.publishEvent(
            new FilesAttachRequestedEvent(taskBodyId, RelatedEntityType.TASK, fileIds, authorId));
    }

    private void publishDetachEvent(Long taskBodyId, List<Long> removedFileIds, Long authorId) {
        if (removedFileIds == null || removedFileIds.isEmpty()) {
            return;
        }

        applicationEventPublisher.publishEvent(
            new FilesDetachRequestedEvent(RelatedEntityType.TASK, taskBodyId, removedFileIds, authorId));
    }

    private void checkOwnerOrAdmin(Long taskBodyOwnerId, Long taskId) {
        if (!securityFacade.hasRole("ADMIN") && !securityFacade.isOwner(taskBodyOwnerId)) {
            throw new TaskAccessRestrictedException(taskId);
        }
    }

    private boolean isOrgOrAdmin(UserAuthDetails userDetails) {
        return userDetails.role().equals(Role.ADMIN) || userDetails.role().equals(Role.ORG);
    }

    private void checkForAssignments(Long taskBodyId) {
        applicationEventPublisher.publishEvent(new TaskDeletionRequestEvent(taskBodyId));
    }
}
