package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateInvitationResponse;

public interface InvitationService {
    CreateInvitationResponse inviteStudents(CreateInvitationRequest request);

}
