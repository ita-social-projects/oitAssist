package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.EnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.EnrollmentRequestsFilter;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.EnrollmentResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessEnrollmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentService<Q extends EnrollmentRequest, R> {
    EnrollmentResponse sendEnrollmentRequest(Q request);

    ProcessEnrollmentResponse acceptRequest(Long requestId);

    ProcessEnrollmentResponse rejectRequest(Long requestId, RejectEnrollmentRequest request);

    ProcessEnrollmentResponse cancelRequest(Long requestId);

    Page<R> getEnrollmentRequests(EnrollmentRequestsFilter request, String search, Pageable pageable);
}
