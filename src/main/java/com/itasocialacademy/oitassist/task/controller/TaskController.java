package com.itasocialacademy.oitassist.task.controller;

import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import com.itasocialacademy.oitassist.task.dao.dto.request.UpdateTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.request.CreateTaskDTO;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/tasks")
@Tag(name = "Tasks V1", description = "Operations related to tasks")
public class TaskController
    extends AbstractRestControllerImpl<Long, CreateTaskDTO, UpdateTaskDTO, ResponseTaskDTO, TaskService> {
    protected TaskController(TaskService service) {
        super(service);
    }
}
