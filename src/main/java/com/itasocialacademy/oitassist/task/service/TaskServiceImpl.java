package com.itasocialacademy.oitassist.task.service;

import com.itasocialacademy.oitassist.filemanager.api.events.FilesAttachRequestedEvent;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dao.repository.TaskBodyRepository;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import com.itasocialacademy.oitassist.task.mapper.TaskBodyMapper;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskBodyRepository taskBodyRepository;
    private final TaskBodyMapper taskBodyMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public TaskResponseDTO createTask(CreateTaskRequestDTO requestDTO) {
        TaskBody createdTask = taskBodyRepository.save(taskBodyMapper.toEntity(requestDTO));
        publishAttachEvent(createdTask.getId(), requestDTO.fileIds(), createdTask.getCreatedBy());

        return taskBodyMapper.toResponse(createdTask);
    }

    // helpers
    private void publishAttachEvent(Long taskBodyId, List<Long> fileIds, Long authorId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        applicationEventPublisher.publishEvent(
            new FilesAttachRequestedEvent(taskBodyId, RelatedEntityType.TASK, fileIds, authorId));
    }
}
