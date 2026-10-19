package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessEnrollmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentService<R> {
    ProcessEnrollmentResponse acceptRequest(Long requestId);

    ProcessEnrollmentResponse rejectRequest(Long requestId, RejectEnrollmentRequest request);

    ProcessEnrollmentResponse cancelRequest(Long requestId);

    Page<R> getEnrollmentRequests(Long competitionId, Long stageId, String search, Pageable pageable);
}
