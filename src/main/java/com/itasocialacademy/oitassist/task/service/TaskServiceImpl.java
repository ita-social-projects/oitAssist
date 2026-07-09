package com.itasocialacademy.oitassist.task.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.filemanager.api.events.FilesAttachRequestedEvent;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dao.repository.TaskBodyRepository;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.task.mapper.TaskBodyMapper;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import java.util.List;
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

    // helpers
    private void publishAttachEvent(Long taskBodyId, List<Long> fileIds, Long authorId) {
        applicationEventPublisher.publishEvent(
            new FilesAttachRequestedEvent(taskBodyId, RelatedEntityType.TASK, fileIds, authorId));
    }
}
