package com.itasocialacademy.oitassist.participation.mapper.interfaces;

import com.itasocialacademy.oitassist.competition.api.dto.EnrollmentCompetitionStageDetail;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.OpenCompetitionEnrollmentResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.UserApplicationSummary;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface ApplicationMapper {
    CreateApplicationResponse toResponse(Application application);

    UserApplicationSummary toUserApplicationSummary(Application application);

    @Mapping(target = "competition", source = "stageDetail.competition")
    @Mapping(target = "stage", source = "stageDetail.stage")
    @Mapping(target = "userApplication", source = "application")
    @Mapping(target = "isParticipant", source = "isParticipant")
    OpenCompetitionEnrollmentResponse toOpenCompetitionEnrollmentResponse(
        EnrollmentCompetitionStageDetail stageDetail,
        Application application,
        boolean isParticipant);
}
