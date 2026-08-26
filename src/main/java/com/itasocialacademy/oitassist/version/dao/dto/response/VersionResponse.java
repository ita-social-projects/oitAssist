package com.itasocialacademy.oitassist.version.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Build version of the running application")
public record VersionResponse(
    @Schema(description = "Backend build information") BackendVersion backend,
    @Schema(description = "Frontend build information") FrontendVersion frontend,
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

    @Schema(description = "Frontend build information")
    public record FrontendVersion(
        @Schema(
            description = "Full hash of the commit the frontend was built from",
            example = "5164fc9928b3cfde344f3320fee540fba1f78873") String commitId,
        @Schema(
            description = "Short hash of the commit the frontend was built from",
            example = "5164fc9") String shortCommitId,
        @Schema(description = "Date and time of the commit") Instant commitTime,
        @Schema(description = "Branch the frontend was built from", example = "dev") String branch,
        @Schema(description = "Frontend version", example = "1.4.2") String version) {
    }
}
