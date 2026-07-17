package com.itasocialacademy.oitassist.participation.mapper.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.model.Invitation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface InvitationMapper
    extends EnrollmentMapper<Invitation, CreateInvitationRequest, CreateInvitationResponse> {
}
