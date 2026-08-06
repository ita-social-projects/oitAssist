package com.itasocialacademy.oitassist.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "DTO for removing task owner")
public record RemoveOwnerRequestDTO(
    @Schema(
        description = "Email address of the task owner.",
        example = "example@mail.com",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Email String ownerEmail) {
}
