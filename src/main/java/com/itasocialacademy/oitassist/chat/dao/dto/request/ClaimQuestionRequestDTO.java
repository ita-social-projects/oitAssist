package com.itasocialacademy.oitassist.chat.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request for claiming a question for review")
public record ClaimQuestionRequestDTO(
    @Schema(
        description = "Expected current question version",
        example = "3",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(
            message = "Question version must not be null") @PositiveOrZero(
                message = "Question version must not be negative") Long version) {
}