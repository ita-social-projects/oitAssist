package com.itasocialacademy.oitassist.user.dao.dto.request;

import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequestDTO(
    @NotNull UpdateRequestStatus status,
    @Size(max = 500) String rejectReason) {
}
