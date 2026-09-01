package com.itasocialacademy.oitassist.chat.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for publishing an official answer")
public record CreateOfficialAnswerRequestDTO(
    @Schema(
        description = "Official answer content",
        example = "The memory limit includes the input and output buffers.",
        maxLength = 10_000,
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(
            message = "Official answer content must not be blank") @Size(max = 10_000,
                message = "Official answer content must not exceed 10000 characters") String content) {
}