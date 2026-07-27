package com.itasocialacademy.oitassist.chat.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for creating a participant question")
public record CreateQuestionRequestDTO(

    @Schema(
        description = "Question title",
        example = "Clarification about input format",
        maxLength = 200,
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "Question title must not be blank") @Size(
            max = 200, message = "Question title must not exceed 200 characters") String title,

    @Schema(
        description = "Question content",
        example = "May the input contain duplicate values?",
        maxLength = 10_000,
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "Question content must not be blank") @Size(
            max = 10_000, message = "Question content must not exceed 10000 characters") String content) {
}
