package com.itasocialacademy.oitassist.task.service.interfaces;

import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    TaskResponseDTO createTask(CreateTaskRequestDTO requestDTO);

    TaskResponseDTO getTaskById(Long id);

    Page<TaskResponseDTO> getAllTasks(Pageable pageable);

    Page<TaskResponseDTO> getAllMyTasks(Pageable pageable);
}
