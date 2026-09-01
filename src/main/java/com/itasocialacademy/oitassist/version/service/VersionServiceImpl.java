package com.itasocialacademy.oitassist.version.service;

import com.itasocialacademy.oitassist.version.dao.dto.response.VersionResponse;
import com.itasocialacademy.oitassist.version.properties.FrontendVersionProperties;
import com.itasocialacademy.oitassist.version.service.interfaces.VersionService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
public class VersionServiceImpl implements VersionService {
    private final GitProperties gitProperties;
    private final BuildProperties buildProperties;
    private final FrontendVersionProperties frontendVersionProperties;

    public VersionServiceImpl(
        ObjectProvider<@NonNull GitProperties> gitPropertiesProvider,
        ObjectProvider<@NonNull BuildProperties> buildPropertiesProvider,
        FrontendVersionProperties frontendVersionProperties) {
        this.gitProperties = gitPropertiesProvider.getIfAvailable();
        this.buildProperties = buildPropertiesProvider.getIfAvailable();
        this.frontendVersionProperties = frontendVersionProperties;

        if (this.gitProperties == null) {
            log.warn("git.properties is missing, commit data will not be reported");
        }
        if (this.buildProperties == null) {
            log.warn("build-info.properties is missing, build data will not be reported");
        }
        if (frontendVersionProperties.commitId() == null) {
            log.warn("frontend-info.properties is missing, frontend build data will not be reported");
        }
    }

    @Override
    public VersionResponse getVersion() {
        return new VersionResponse(getBackendVersion(), getFrontendVersion(), getBuildTime());
    }

    private VersionResponse.BackendVersion getBackendVersion() {
        if (gitProperties == null) {
            return new VersionResponse.BackendVersion(null, null, null, null, null);
        }
        return new VersionResponse.BackendVersion(
            gitProperties.getCommitId(),
            gitProperties.getShortCommitId(),
            gitProperties.getCommitTime(),
            gitProperties.getBranch(),
            getArtifactVersion());
    }

    private VersionResponse.FrontendVersion getFrontendVersion() {
        return new VersionResponse.FrontendVersion(
            frontendVersionProperties.commitId(),
            frontendVersionProperties.shortCommitId(),
            parseCommitTime(frontendVersionProperties.commitTime()),
            frontendVersionProperties.branch(),
            frontendVersionProperties.version());
    }

    private String getArtifactVersion() {
        return buildProperties == null ? null : buildProperties.getVersion();
    }

    private Instant getBuildTime() {
        return buildProperties == null ? null : buildProperties.getTime();
    }

    private static Instant parseCommitTime(String commitTime) {
        if (commitTime == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(commitTime).toInstant();
        } catch (DateTimeParseException e) {
            log.warn("Frontend commit time '{}' is not a valid date, it will not be reported", commitTime);
            return null;
        }
    }
}
