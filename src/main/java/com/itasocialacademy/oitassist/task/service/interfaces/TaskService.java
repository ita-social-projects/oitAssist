package com.itasocialacademy.oitassist.task.service.interfaces;

import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import com.itasocialacademy.oitassist.task.dao.dto.request.CreateTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.request.UpdateTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;

public interface TaskService extends BaseService<Long, CreateTaskDTO, UpdateTaskDTO, ResponseTaskDTO> {
}
