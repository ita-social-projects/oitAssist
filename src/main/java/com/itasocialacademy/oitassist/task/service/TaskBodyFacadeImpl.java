package com.itasocialacademy.oitassist.task.service;

import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.task.api.TaskBodyFacade;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class TaskBodyFacadeImpl implements TaskBodyFacade {
    private final TaskService taskService;

    @Override
    public Optional<TaskBodyDetail> findTaskBodyById(Long taskBodyId) {
        return taskService.getTaskBodyDetailById(taskBodyId);
    }

    @Override
    public Map<Long, String> getTaskTitlesByIds(List<Long> taskBodyIds) {
        return taskService.getTaskTitlesByIds(taskBodyIds);
    }

    @Override
    public TaskBodyDetail createTask(String title, String description, List<Long> fileIds) {
        CreateTaskRequestDTO request = new CreateTaskRequestDTO(title, description, fileIds);
        TaskResponseDTO created = taskService.createTask(request);
        return TaskBodyDetail.builder()
            .id(created.id())
            .title(created.title())
            .description(created.description())
            .ownerIds(created.ownerIds())
            .build();
    }
}