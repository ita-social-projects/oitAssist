package com.itasocialacademy.oitassist.school.mapper;

import com.itasocialacademy.oitassist.school.api.dto.RegionDetail;
import com.itasocialacademy.oitassist.school.dao.dto.response.RegionResponse;
import com.itasocialacademy.oitassist.school.dao.model.Region;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RegionMapper {
    RegionResponse toResponse(Region entity);

    RegionDetail toDetail(Region entity);
}
