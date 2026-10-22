package com.itasocialacademy.oitassist.submission.dao.dto.response;

import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Schema(description = "DTO representing a Submission entity response")
@Builder
public record SubmissionResponseDTO(
    @Schema(
        description = "Unique identifier of the submission",
        example = "1") Long id,

    @Schema(
        description = "Comment attached to the submission",
        example = "I think list is better than set in this example") String comment,

    @Schema(
        description = "Files attached to the submission") List<FileDetailsDTO> files,

    @Schema(
        description = "Id of submission sender",
        example = "1") Long submittedBy,

    @Schema(
        description = "Id of the task assignment which submission was send for",
        example = "1") Long taskAssignmentId,

    @Schema(
        description = "Submission`s date and time of sending") Instant submittedAt) {
}