package com.itasocialacademy.oitassist.task.service.interfaces;

import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;

public interface TaskService {
    TaskResponseDTO createTask(CreateTaskRequestDTO requestDTO);

    TaskResponseDTO getTaskById(Long id);
}
