package com.itasocialacademy.oitassist.school.mapper;

import com.itasocialacademy.oitassist.school.api.dto.SchoolDetail;
import com.itasocialacademy.oitassist.school.dao.dto.response.SchoolResponse;
import com.itasocialacademy.oitassist.school.dao.model.School;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolMapper {
    @Mapping(target = "districtCode", source = "district.code")
    SchoolResponse toResponse(School entity);

    @Mapping(target = "districtCode", source = "district.code")
    @Mapping(target = "districtName", source = "district.name")
    @Mapping(target = "regionCode", source = "district.region.code")
    @Mapping(target = "regionName", source = "district.region.name")
    SchoolDetail toDetail(School entity);
}
