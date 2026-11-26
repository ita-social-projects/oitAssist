package com.itasocialacademy.oitassist.school.mapper;

import com.itasocialacademy.oitassist.school.api.dto.DistrictDetail;
import com.itasocialacademy.oitassist.school.dao.dto.response.DistrictResponse;
import com.itasocialacademy.oitassist.school.dao.model.District;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DistrictMapper {
    @Mapping(target = "regionCode", source = "region.code")
    DistrictResponse toResponse(District entity);

    @Mapping(target = "regionCode", source = "region.code")
    DistrictDetail toDetail(District entity);
}
