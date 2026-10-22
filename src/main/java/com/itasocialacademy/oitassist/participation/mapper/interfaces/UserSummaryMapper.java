package com.itasocialacademy.oitassist.participation.mapper.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.response.UserSummary;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserSummaryMapper {
    UserSummary toUserSummary(UserProfileDetails userProfile);
}
