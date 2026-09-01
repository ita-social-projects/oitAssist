package com.itasocialacademy.oitassist.task.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.task.api.events.TaskDeletionRequestEvent;
import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dao.model.TaskOwner;
import com.itasocialacademy.oitassist.task.dao.model.TaskTitleView;
import com.itasocialacademy.oitassist.task.dao.model.id.TaskOwnerId;
import com.itasocialacademy.oitassist.task.dao.repository.TaskBodyRepository;
import com.itasocialacademy.oitassist.task.dto.request.AddOwnerRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.RemoveOwnerRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.UpdateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import com.itasocialacademy.oitassist.task.exceptions.StaleTaskVersionException;
import com.itasocialacademy.oitassist.task.exceptions.TaskAccessRestrictedException;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.task.mapper.TaskBodyMapper;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {
    private final TaskBodyRepository taskBodyRepository;
    private final TaskBodyMapper taskBodyMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SecurityFacade securityFacade;
    private final UserFacade userFacade;
    private final FileManagerFacade fileManagerFacade;
    private static final String ADMIN_ROLE = "ADMIN";

    @Override
    @Transactional
    public TaskResponseDTO createTask(
        CreateTaskRequestDTO requestDTO,
        List<MultipartFile> problemFiles,
        List<MultipartFile> referenceFiles,
        List<MultipartFile> solutionFiles) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to create tasks",
                ErrorCode.ACCESS_DENIED));

        TaskBody task = taskBodyMapper.toEntity(requestDTO);

        TaskBody createdTask = taskBodyRepository.save(task);

        TaskOwner owner = TaskOwner.builder()
            .id(new TaskOwnerId(createdTask.getId(), currentUserId))
            .build();

        createdTask.addOwner(owner);

        log.debug("Created Task: Id {}; Title - {}", createdTask.getId(), createdTask.getTitle());

        uploadFilesByRole(createdTask.getId(), problemFiles, FileRole.PROBLEM);
        uploadFilesByRole(createdTask.getId(), referenceFiles, FileRole.REFERENCE);
        uploadFilesByRole(createdTask.getId(), solutionFiles, FileRole.SOLUTION);

        return getResponse(createdTask);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDTO getTaskById(Long id) {
        TaskBody taskBody = taskBodyRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));

        checkOwnerOrAdmin(taskBody.getOwners().stream()
            .map(o -> o.getId().getOwnerId()).collect(Collectors.toSet()),
            taskBody.getId());

        log.debug("Get Task: Id {}", taskBody.getId());
        return getResponse(taskBody);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponseDTO> getAllTasks(Pageable pageable, String search) {
        log.debug("getAllTasks: page={}, size={}, sort={} search={}",
            pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort(), search);

        String normalizedSearch = getNormalizedSearch(search);

        Page<TaskBody> tasksPage = taskBodyRepository.findAllByTitleLike(normalizedSearch, pageable);

        return getResponseBulk(tasksPage);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponseDTO> getAllMyTasks(Pageable pageable, String search) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to view created tasks",
                ErrorCode.ACCESS_DENIED));
        log.debug("getAllMyTasks: userId={}, page={}, size={}, sort={}, search={}",
            currentUserId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort(), search);

        String normalizedSearch = getNormalizedSearch(search);

        Page<TaskBody> myTasksPage = taskBodyRepository.findAllByOwnerId(currentUserId, normalizedSearch, pageable);

        return getResponseBulk(myTasksPage);
    }

    @Override
    @Transactional
    public TaskResponseDTO updateTask(
        Long taskId,
        UpdateTaskRequestDTO requestDTO,
        List<MultipartFile> problemFiles,
        List<MultipartFile> referenceFiles,
        List<MultipartFile> solutionFiles) {
        TaskBody existingTask = taskBodyRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        checkOwnerOrAdmin(existingTask.getOwners().stream()
            .map(o -> o.getId().getOwnerId()).collect(Collectors.toSet()),
            existingTask.getId());

        checkTaskVersion(existingTask.getVersion(), requestDTO.version(), existingTask.getId());

        existingTask.setTitle(requestDTO.title());
        existingTask.setDescription(requestDTO.description());

        TaskBody updatedTask = taskBodyRepository.saveAndFlush(existingTask);
        log.debug("Updated Task: Id {}, Title - {}", updatedTask.getId(), updatedTask.getTitle());

        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to update tasks",
                ErrorCode.ACCESS_DENIED));

        if (requestDTO.removedFileIds() != null && !requestDTO.removedFileIds().isEmpty()) {
            fileManagerFacade.detachFiles(
                RelatedEntityType.TASK, updatedTask.getId(), requestDTO.removedFileIds(), currentUserId);
        }

        if (requestDTO.roleUpdates() != null) {
            requestDTO.roleUpdates().forEach(fileManagerFacade::updateFileRole);
        }

        uploadFilesByRole(updatedTask.getId(), problemFiles, FileRole.PROBLEM);
        uploadFilesByRole(updatedTask.getId(), referenceFiles, FileRole.REFERENCE);
        uploadFilesByRole(updatedTask.getId(), solutionFiles, FileRole.SOLUTION);

        return getResponse(updatedTask);
    }

    @Override
    @Transactional
    public TaskResponseDTO addTaskOwner(Long taskId, AddOwnerRequestDTO addOwnerRequest) {
        if (!securityFacade.hasRole(ADMIN_ROLE)) {
            throw new TaskAccessRestrictedException(taskId);
        }

        TaskBody task = taskBodyRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        UserAuthDetails userDetails = userFacade.findByEmail(addOwnerRequest.newOwnerEmail())
            .orElseThrow(UserNotFoundException::new);

        if (!isOrgOrAdmin(userDetails)) {
            throw new ValidationException("Provided user is not ADMIN nor ORG", ErrorCode.COMMON_VALIDATION_FAILED);
        }

        checkTaskVersion(task.getVersion(), addOwnerRequest.version(), task.getId());

        if (task.getOwners().stream()
            .anyMatch(owner -> owner.getId().getOwnerId().equals(userDetails.id()))) {
            return getResponse(task);
        }

        TaskOwner owner = TaskOwner.builder()
            .id(new TaskOwnerId(task.getId(), userDetails.id()))
            .build();

        task.addOwner(owner);
        auditOwnersUpdate(task);

        log.debug("User {} added to task`s {} owners", userDetails.id(), task.getId());

        return getResponse(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO removeTaskOwner(Long taskId, RemoveOwnerRequestDTO removeOwnerRequest) {
        if (!securityFacade.hasRole(ADMIN_ROLE)) {
            throw new TaskAccessRestrictedException(taskId);
        }

        TaskBody task = taskBodyRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        UserAuthDetails userDetails = userFacade.findByEmail(removeOwnerRequest.ownerEmail())
            .orElseThrow(UserNotFoundException::new);

        checkTaskVersion(task.getVersion(), removeOwnerRequest.version(), task.getId());

        Optional<TaskOwner> toRemove = task.getOwners().stream()
            .filter(o -> o.getId().getOwnerId().equals(userDetails.id())).findFirst();

        if (toRemove.isEmpty()) {
            return getResponse(task);
        }

        if (task.getOwners().size() == 1) {
            throw new ValidationException(
                "Cannot remove the last owner of a task",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }

        task.removeOwner(toRemove.get());
        auditOwnersUpdate(task);

        log.debug("User {} removed from task`s {} owners", userDetails.id(), task.getId());

        return getResponse(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        TaskBody taskToDelete = taskBodyRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        checkOwnerOrAdmin(taskToDelete.getOwners().stream()
            .map(o -> o.getId().getOwnerId()).collect(Collectors.toSet()),
            taskToDelete.getId());

        checkForAssignments(taskId);

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
    private void uploadFilesByRole(Long taskId, List<MultipartFile> files, FileRole role) {
        if (files == null || files.isEmpty()) {
            return;
        }
        fileManagerFacade.uploadFiles(files, RelatedEntityType.TASK, taskId, role);
    }

    private void checkOwnerOrAdmin(Set<Long> taskBodyOwnerIds, Long taskId) {
        Optional<Long> currentUserId = securityFacade.getCurrentUserId();
        if (currentUserId.isEmpty() || (!securityFacade.hasRole(ADMIN_ROLE)
            && !taskBodyOwnerIds.contains(currentUserId.get()))) {
            throw new TaskAccessRestrictedException(taskId);
        }
    }

    private boolean isOrgOrAdmin(UserAuthDetails userDetails) {
        return userDetails.role().equals(Role.ADMIN) || userDetails.role().equals(Role.ORG);
    }

    private void checkForAssignments(Long taskBodyId) {
        applicationEventPublisher.publishEvent(new TaskDeletionRequestEvent(taskBodyId));
    }

    private List<FileDetailsDTO> getTaskFiles(Long taskBodyId) {
        Set<FileRole> allowedFileRoles = Set.of(FileRole.PROBLEM, FileRole.REFERENCE, FileRole.SOLUTION);
        return fileManagerFacade.getFilesByEntity(RelatedEntityType.TASK, taskBodyId, allowedFileRoles);
    }

    private Map<Long, List<FileDetailsDTO>> getTaskFilesBulk(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return fileManagerFacade.getFilesByEntities(
            RelatedEntityType.TASK,
            taskIds,
            Set.of(FileRole.PROBLEM, FileRole.REFERENCE, FileRole.SOLUTION));
    }

    private Page<TaskResponseDTO> getResponseBulk(Page<TaskBody> myTasksPage) {
        List<Long> taskIds = myTasksPage.getContent().stream()
            .map(TaskBody::getId)
            .toList();

        Map<Long, List<FileDetailsDTO>> files = getTaskFilesBulk(taskIds);

        List<Long> creatorIds = myTasksPage.getContent().stream().map(TaskBody::getCreatedBy).toList();
        Map<Long, String> creatorsEmails = getUserEmailsByIdsInBulk(creatorIds);

        return myTasksPage
            .map(e -> taskBodyMapper.toResponse(e, files.getOrDefault(e.getId(), Collections.emptyList()),
                creatorsEmails.get(e.getCreatedBy())));
    }

    private TaskResponseDTO getResponse(TaskBody taskBody) {
        return taskBodyMapper.toResponse(taskBody, getTaskFiles(taskBody.getId()),
            getUserEmailById(taskBody.getCreatedBy()));
    }

    private String getUserEmailById(Long userId) {
        return userFacade.findProfileById(userId).orElseThrow(UserNotFoundException::new).email();
    }

    private Map<Long, String> getUserEmailsByIdsInBulk(List<Long> userIds) {
        return userFacade.findByIds(userIds).stream()
            .collect(Collectors.toMap(UserAuthDetails::id, UserAuthDetails::email));
    }

    private String getNormalizedSearch(String search) {
        return search == null || search.isBlank()
            ? ""
            : search.trim()
                .replaceAll("\\s+", " ")
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private void checkTaskVersion(Long actualVersion, Long providedVersion, Long taskId) {
        if (!Objects.equals(actualVersion, providedVersion)) {
            throw new StaleTaskVersionException(taskId);
        }
    }

    private void auditOwnersUpdate(TaskBody taskBody) {
        taskBody.setUpdatedAt(Instant.now());
        taskBody.setUpdatedBy(securityFacade.getCurrentUserId().orElseThrow(UserNotFoundException::new));
        taskBodyRepository.saveAndFlush(taskBody);
    }
}
