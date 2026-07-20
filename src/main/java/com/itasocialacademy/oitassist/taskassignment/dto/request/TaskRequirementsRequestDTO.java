package com.itasocialacademy.oitassist.taskassignment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TaskRequirementsRequestDTO(
    @NotEmpty @Valid List<RequiredFileRequest> requiredFileList) {
    public record RequiredFileRequest(
        @NotBlank String prompt,

        @NotBlank String namingRule,

        @NotEmpty List<String> allowedExtensions,

        @NotNull @Min(1) Integer maxFileSizeMb) {
    }
}
