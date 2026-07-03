package com.itasocialacademy.oitassist.competition.mapper;

import com.itasocialacademy.oitassist.competition.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StageMapper {
    Stage toEntity(CreateStageRequest request);

    StageResponse toResponse(Stage entity);
}
