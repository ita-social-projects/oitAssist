package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Інформація про заявку поточного учня")
@Builder
public record UserApplicationSummary(
    @Schema(description = "ID заявки (використовується для скасування)", example = "42")
    Long id,

    @Schema(description = "Поточний статус заявки", example = "PENDING")
    RequestStatus status) {
}

