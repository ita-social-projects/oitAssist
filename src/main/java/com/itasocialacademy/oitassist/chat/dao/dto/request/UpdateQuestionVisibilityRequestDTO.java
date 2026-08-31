package com.itasocialacademy.oitassist.chat.dao.dto.request;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request for changing question visibility")
public record UpdateQuestionVisibilityRequestDTO(
    @Schema(
        description = "Requested question visibility",
        example = "PUBLIC",
        allowableValues = {
            "PRIVATE", "PUBLIC"},
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(
            message = "Question visibility must not be null") QuestionVisibility visibility,

    @Schema(
        description = "Expected current question version",
        example = "3",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(
            message = "Question version must not be null") @PositiveOrZero(
                message = "Question version must not be negative") Long version){
}