package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.AcceptApplicationListRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectApplicationListRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.AcceptedApplicationListResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ApplicationListItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.EnrollmentResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.RejectedApplicationListResponse;

public interface ApplicationService extends EnrollmentService<ApplicationListItemResponse> {
    EnrollmentResponse sendApplicationRequest(Long competitionId, Long stageId);

    AcceptedApplicationListResponse acceptApplications(
        AcceptApplicationListRequest request,
        Long competitionId,
        Long stageId);

    RejectedApplicationListResponse rejectApplications(
        RejectApplicationListRequest request,
        Long competitionId,
        Long stageId);
}
