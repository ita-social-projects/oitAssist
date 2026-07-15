package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateApplicationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessApplicationResponse;

public interface ApplicationService {
    CreateApplicationResponse userApply(CreateApplicationRequest createApplicationRequest);

    ProcessApplicationResponse acceptUserApplication(Long applicationId);

    ProcessApplicationResponse rejectUserApplication(Long applicationId, RejectEnrollmentRequest request);

    ProcessApplicationResponse cancelUserApplication(Long applicationId);
}
