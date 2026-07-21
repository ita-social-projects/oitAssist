package com.itasocialacademy.oitassist.task.mapper;

import com.itasocialacademy.oitassist.task.api.dto.TaskForumContext;
import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskBodyMapper {
    TaskBody toEntity(CreateTaskRequestDTO taskBody);

    TaskResponseDTO toResponse(TaskBody taskBody);

    @Mapping(target = "taskId", source = "id")
    TaskForumContext toForumContext(TaskBody taskBody);
}
