package com.itasocialacademy.oitassist.task.mapper;

import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskBodyMapper {
    TaskBody toEntity(CreateTaskRequestDTO taskBody);

    TaskResponseDTO toResponse(TaskBody taskBody, List<FileDetailsDTO> files);

    TaskBodyDetail toTaskBodyDetail(TaskBody taskBody);
}
