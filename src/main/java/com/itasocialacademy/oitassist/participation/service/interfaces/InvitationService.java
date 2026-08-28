package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.InvitationListItemResponse;

public interface InvitationService extends EnrollmentService<CreateInvitationRequest, InvitationListItemResponse> {
}
