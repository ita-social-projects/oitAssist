package com.itasocialacademy.oitassist.submission.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "DTO for creating a new submission")
public record SubmissionCreateRequest(
    @Schema(
        description = "Optional comment for the submission",
        example = "I think list is better than set in this example",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) String comment,

    @Schema(
        description = "Id of the task assignment which submission was send for",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED) Long taskAssignmentId,

    @Schema(
        description = "Files attached to the submission",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty(
        message = "At least one file must be provided") List<MultipartFile> files) {
}
