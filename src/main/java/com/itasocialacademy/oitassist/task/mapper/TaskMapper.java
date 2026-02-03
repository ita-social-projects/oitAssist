package com.itasocialacademy.oitassist.task.mapper;

import com.itasocialacademy.oitassist.core.rest.mapper.GeneralMapper;
import com.itasocialacademy.oitassist.task.dao.dto.request.UpdateTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import com.itasocialacademy.oitassist.task.dao.dto.request.CreateTaskDTO;
import com.itasocialacademy.oitassist.task.dao.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TaskMapper extends GeneralMapper<Task, CreateTaskDTO, UpdateTaskDTO, ResponseTaskDTO> {
    @Override
    ResponseTaskDTO toDTO(Task task);

    @Override
    Task toEntity(CreateTaskDTO taskDTO);

    @Override
    void merge(UpdateTaskDTO taskDTO, @MappingTarget Task task);
}
