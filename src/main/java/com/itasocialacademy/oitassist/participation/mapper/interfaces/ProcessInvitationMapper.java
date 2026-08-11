package com.itasocialacademy.oitassist.participation.mapper.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.model.Invitation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface ProcessInvitationMapper extends ProcessEnrollmentMapper<ProcessInvitationResponse, Invitation> {
}
