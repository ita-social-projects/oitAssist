package com.itasocialacademy.oitassist.task.service;

import com.itasocialacademy.oitassist.core.rest.service.AbstractServiceImpl;
import com.itasocialacademy.oitassist.task.dao.dto.request.CreateTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.request.UpdateTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import com.itasocialacademy.oitassist.task.dao.model.Task;
import com.itasocialacademy.oitassist.task.dao.repository.TaskRepository;
import com.itasocialacademy.oitassist.task.mapper.TaskMapper;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl
    extends AbstractServiceImpl<Long, Task, CreateTaskDTO, UpdateTaskDTO, ResponseTaskDTO, TaskRepository, TaskMapper>
    implements TaskService {
    protected TaskServiceImpl(TaskRepository repository, TaskMapper mapper) {
        super(repository, mapper);
    }
}
