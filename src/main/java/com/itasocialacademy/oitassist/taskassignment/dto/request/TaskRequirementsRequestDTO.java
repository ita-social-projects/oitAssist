package com.itasocialacademy.oitassist.taskassignment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "DTO specifying file requirements for task submissions")
public record TaskRequirementsRequestDTO(
    @Schema(
        description = "List of required file specifications for task submission",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty @Valid List<RequiredFileRequest> requiredFiles) {
    public record RequiredFileRequest(
        @Schema(
            description = "Instructions or prompt describing the file requirements",
            example = "Файл розв'язку до задачі про графи",
            requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String prompt,

        @Schema(
            description = "Naming convention or pattern for the file",
            example = "PowerPoint_РіздвянаЗірка",
            requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String namingRule,

        @Schema(
            description = "List of allowed file extensions for this requirement",
            example = "[\".pptx\", \".docx\", \".xlsx\"]",
            requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty List<@NotBlank String> allowedExtensions,

        @Schema(
            description = "Maximum file size in megabytes",
            example = "50",
            requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Min(1) @Max(200) Integer maxFileSizeMb) {
    }
}
