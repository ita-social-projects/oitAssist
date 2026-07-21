package com.itasocialacademy.oitassist.taskassignment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record TaskRequirementsRequestDTO(
    @NotEmpty @Valid List<RequiredFileRequest> requiredFiles) {
    public record RequiredFileRequest(
        @NotBlank String prompt,

        @NotBlank String namingRule,

        @NotEmpty List<@NotBlank String> allowedExtensions,

        @NotNull @Min(1) @Max(200) Integer maxFileSizeMb) {
    }
}
