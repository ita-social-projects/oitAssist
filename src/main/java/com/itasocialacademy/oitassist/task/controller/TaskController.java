package com.itasocialacademy.oitassist.task.controller;

import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import com.itasocialacademy.oitassist.task.dao.dto.request.UpdateTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.request.CreateTaskDTO;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/tasks")
public class TaskController
    extends AbstractRestControllerImpl<Long, CreateTaskDTO, UpdateTaskDTO, ResponseTaskDTO, TaskService> {
    protected TaskController(TaskService service) {
        super(service);
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<ResponseTaskDTO> getById(@PathVariable Long id) {
        return super.getById(id);
    }

    @PostMapping
    @Override
    public ResponseEntity<ResponseTaskDTO> save(@RequestBody CreateTaskDTO createTaskDTO) {
        return super.save(createTaskDTO);
    }

    @PutMapping
    @Override
    public ResponseEntity<ResponseTaskDTO> update(@RequestBody UpdateTaskDTO updateTaskDTO) {
        return super.update(updateTaskDTO);
    }

    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return super.delete(id);
    }
}
