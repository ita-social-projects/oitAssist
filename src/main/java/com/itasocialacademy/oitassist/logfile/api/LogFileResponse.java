package com.itasocialacademy.oitassist.logfile.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Application log file information")
public record LogFileResponse(
    @Schema(
        description = "File name without the absolute path",
        example = "application.log") String fileName,

    @Schema(
        description = "File size in bytes",
        example = "153920") long size,

    @Schema(
        description = "Date and time when file was last modified") Instant lastModified) {
}
