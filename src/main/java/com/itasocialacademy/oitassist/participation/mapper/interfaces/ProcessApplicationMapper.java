package com.itasocialacademy.oitassist.participation.mapper.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProcessApplicationMapper {
    ProcessApplicationResponse toResponse(Application application);
}
