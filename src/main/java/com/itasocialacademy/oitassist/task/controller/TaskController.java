package com.itasocialacademy.oitassist.task.controller;

import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import com.itasocialacademy.oitassist.task.dao.dto.request.UpdateTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.request.CreateTaskDTO;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks V1", description = "Operations related to tasks")
public class TaskController
    extends AbstractRestControllerImpl<Long, CreateTaskDTO, UpdateTaskDTO, ResponseTaskDTO, TaskService> {
    protected TaskController(TaskService service) {
        super(service);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG', 'AUTHOR')")
    @Override
    public ResponseEntity<ResponseTaskDTO> save(@RequestBody CreateTaskDTO createDto) {
        return super.save(createDto);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG', 'AUTHOR')")
    @Override
    public ResponseEntity<ResponseTaskDTO> update(@RequestBody UpdateTaskDTO updateDTO) {
        return super.update(updateDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG', 'AUTHOR')")
    @Override
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return super.delete(id);
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<ResponseTaskDTO> getById(@PathVariable Long id) {
        return super.getById(id);
    }
}
