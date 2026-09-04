package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.response.ApplicationListItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.EnrollmentResponse;

public interface ApplicationService extends EnrollmentService<ApplicationListItemResponse> {
    EnrollmentResponse sendApplicationRequest(Long competitionId, Long stageId);
}
