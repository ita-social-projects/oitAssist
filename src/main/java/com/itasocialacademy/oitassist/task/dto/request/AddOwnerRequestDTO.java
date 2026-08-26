package com.itasocialacademy.oitassist.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DTO for adding task owner")
public record AddOwnerRequestDTO(
    @Schema(
        description = "Email address of the new task owner. The user must have ADMIN or ORG role",
        example = "example@mail.com",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Email String newOwnerEmail,

    @Schema(description = "Optimistic locking version; must be echoed back on updates",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long version) {
}
