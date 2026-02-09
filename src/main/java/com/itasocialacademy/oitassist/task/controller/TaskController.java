package com.itasocialacademy.oitassist.task.controller;

import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import com.itasocialacademy.oitassist.task.dao.dto.request.UpdateTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.request.CreateTaskDTO;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/tasks")
public class TaskController
    extends AbstractRestControllerImpl<Long, CreateTaskDTO, UpdateTaskDTO, ResponseTaskDTO, TaskService> {
    protected TaskController(TaskService service) {
        super(service);
    }
}
