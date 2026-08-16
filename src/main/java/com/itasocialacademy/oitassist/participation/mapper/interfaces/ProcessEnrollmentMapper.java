package com.itasocialacademy.oitassist.participation.mapper.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessEnrollmentResponse;
import com.itasocialacademy.oitassist.participation.dao.model.ParticipationRequestEvent;

public interface ProcessEnrollmentMapper<R extends ProcessEnrollmentResponse, E extends ParticipationRequestEvent> {
    R toResponse(E requestEvent);
}
