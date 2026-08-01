package com.itasocialacademy.oitassist.chat.dao.dto.request;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request for changing question review status")
public record UpdateQuestionStatusRequestDTO(
    @Schema(
        description = "Requested question review status",
        example = "IN_REVIEW",
        allowableValues = {
            "NEW", "IN_REVIEW", "ANSWERED"},
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(
            message = "Question status must not be null") QuestionStatus status,

    @Schema(
        description = "Expected current question version",
        example = "3",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(
            message = "Question version must not be null") @PositiveOrZero(
                message = "Question version must not be negative") Long version){
}