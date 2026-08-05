package com.itasocialacademy.oitassist.task.mapper;

import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dao.model.TaskOwner;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskBodyMapper {
    TaskBody toEntity(CreateTaskRequestDTO taskBody);

    @Mapping(target = "ownerIds", source = "taskBody.owners")
    TaskResponseDTO toResponse(TaskBody taskBody, List<FileDetailsDTO> files);

    @Mapping(target = "ownerIds", source = "owners")
    TaskBodyDetail toTaskBodyDetail(TaskBody taskBody);

    default Set<Long> mapOwners(Set<TaskOwner> owners) {
        if (owners == null) {
            return Collections.emptySet();
        }

        return owners.stream()
            .map(owner -> owner.getId().getOwnerId())
            .collect(Collectors.toSet());
    }
}
