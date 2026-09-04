package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;

public record SucceededApplicationRejectingItemResponse(
    Long applicationId,
    RequestStatus status) {
}
