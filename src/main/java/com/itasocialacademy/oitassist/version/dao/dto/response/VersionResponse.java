package com.itasocialacademy.oitassist.version.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Build version of the running application")
public record VersionResponse(
    @Schema(description = "Backend build information") BackendVersion backend,
    @Schema(description = "Date and time when the artifact was built") Instant buildTime) {
    @Schema(description = "Backend build information")
    public record BackendVersion(
        @Schema(
            description = "Full hash of the commit the artifact was built from",
            example = "3534bab24568a602605078b9264711223f218dd2") String commitId,
        @Schema(
            description = "Short hash of the commit the artifact was built from",
            example = "3534bab") String shortCommitId,
        @Schema(description = "Date and time of the commit") Instant commitTime,
        @Schema(description = "Branch the artifact was built from", example = "dev") String branch,
        @Schema(description = "Artifact version", example = "0.0.1-SNAPSHOT") String version) {
    }
}
