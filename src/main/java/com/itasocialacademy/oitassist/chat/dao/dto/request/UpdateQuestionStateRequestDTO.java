package com.itasocialacademy.oitassist.chat.dao.dto.request;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request for changing question lifecycle state")
public record UpdateQuestionStateRequestDTO(
    @Schema(
        description = "Requested question lifecycle state",
        example = "CLOSED",
        allowableValues = {
            "OPEN", "CLOSED"},
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(
            message = "Question state must not be null") QuestionState state,

    @Schema(
        description = "Expected current question version",
        example = "3",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(
            message = "Question version must not be null") @PositiveOrZero(
                message = "Question version must not be negative") Long version){
}