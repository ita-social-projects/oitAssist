package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateApplicationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ApplicationListItemResponse;

public interface ApplicationService extends EnrollmentService<CreateApplicationRequest, ApplicationListItemResponse> {
}
