package com.itasocialacademy.oitassist.version.service;

import com.itasocialacademy.oitassist.version.api.VersionResponse;
import com.itasocialacademy.oitassist.version.service.interfaces.VersionService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Slf4j
@Service
public class VersionServiceImpl implements VersionService {
    private final GitProperties gitProperties;
    private final BuildProperties buildProperties;

    public VersionServiceImpl(
        ObjectProvider<@NonNull GitProperties> gitPropertiesProvider,
        ObjectProvider<@NonNull BuildProperties> buildPropertiesProvider) {
        this.gitProperties = gitPropertiesProvider.getIfAvailable();
        this.buildProperties = buildPropertiesProvider.getIfAvailable();

        if (this.gitProperties == null) {
            log.warn("git.properties is missing, commit data will not be reported");
        }
        if (this.buildProperties == null) {
            log.warn("build-info.properties is missing, build data will not be reported");
        }
    }

    @Override
    public VersionResponse getVersion() {
        return new VersionResponse(getBackendVersion(), getBuildTime());
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

    private String getArtifactVersion() {
        return buildProperties == null ? null : buildProperties.getVersion();
    }

    private Instant getBuildTime() {
        return buildProperties == null ? null : buildProperties.getTime();
    }
}
