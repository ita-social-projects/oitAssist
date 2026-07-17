package com.itasocialacademy.oitassist.participation.mapper.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.EnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.EnrollmentResponse;
import com.itasocialacademy.oitassist.participation.dao.model.ParticipationRequestEvent;
import java.util.List;

public interface EnrollmentMapper<E extends ParticipationRequestEvent, Q extends EnrollmentRequest,
    R extends EnrollmentResponse> {
    E toEntity(Q request);

    R toResponse(E event);

    R toResponse(List<E> events);
}
