package com.itasocialacademy.oitassist.chat.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for adding a participant comment")
public record CreateCommentRequestDTO(

        @Schema(
                description = "Comment content",
                example = "Could you also clarify the memory limit?",
                maxLength = 10_000,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Comment content must not be blank")
        @Size(
                max = 10_000,
                message = "Comment content must not exceed 10000 characters")
        String content) {
}