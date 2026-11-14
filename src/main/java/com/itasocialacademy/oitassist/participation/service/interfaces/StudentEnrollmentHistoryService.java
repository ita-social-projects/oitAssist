package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.response.ApplicationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.InvitationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ParticipationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentEnrollmentHistoryService {
    Page<InvitationHistoryItemResponse> getInvitations(Long userId, RequestStatus status, Pageable pageable);

    Page<ApplicationHistoryItemResponse> getApplications(Long userId, RequestStatus status, Pageable pageable);

    Page<ParticipationHistoryItemResponse> getParticipations(Long userId, Pageable pageable);
}
