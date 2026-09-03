package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.AcceptApplicationsRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.AcceptedApplicationListResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ApplicationListItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.EnrollmentResponse;

public interface ApplicationService extends EnrollmentService<ApplicationListItemResponse> {
    EnrollmentResponse sendApplicationRequest(Long competitionId, Long stageId);

    AcceptedApplicationListResponse acceptApplications(
        AcceptApplicationsRequest request,
        Long competitionId,
        Long stageId);
}
