package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.EligibleStudentResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.EnrollmentResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.InvitationListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvitationService extends EnrollmentService<InvitationListItemResponse> {
    EnrollmentResponse sendInvitationRequests(
        Long competitionId,
        Long stageId,
        CreateInvitationRequest invitationRequest);

    Page<EligibleStudentResponse> getEligibleStudents(
        Long competitionId,
        Long stageId,
        String search,
        Pageable pageable);
}
