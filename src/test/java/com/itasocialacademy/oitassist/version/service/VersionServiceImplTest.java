package com.itasocialacademy.oitassist.version.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.version.dao.dto.response.VersionResponse;
import com.itasocialacademy.oitassist.version.properties.FrontendVersionProperties;
import java.time.Instant;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;

@ExtendWith(MockitoExtension.class)
class VersionServiceImplTest {

    private static final String COMMIT_ID = "3534bab24568a602605078b9264711223f218dd2";
    private static final String SHORT_COMMIT_ID = "3534bab";
    private static final String BRANCH = "dev";
    private static final String ARTIFACT_VERSION = "0.0.1-SNAPSHOT";
    private static final Instant COMMIT_TIME = Instant.parse("2026-08-19T08:22:05Z");
    private static final Instant BUILD_TIME = Instant.parse("2026-08-19T19:55:29.490Z");
    private static final String FRONTEND_COMMIT_ID = "9f1c2ab7d4e58306b1c0f2a4d7e93b5c8a6d1e42";
    private static final String FRONTEND_SHORT_COMMIT_ID = "9f1c2ab";
    private static final String FRONTEND_VERSION = "1.4.2";
    private static final Instant FRONTEND_COMMIT_TIME = Instant.parse("2026-08-21T10:15:00Z");

    @Mock
    private ObjectProvider<GitProperties> gitPropertiesProvider;

    @Mock
    private ObjectProvider<BuildProperties> buildPropertiesProvider;

    @Test
    void getVersion_ShouldReturnFullVersion_WhenBuildMetadataIsPresent() {
        when(gitPropertiesProvider.getIfAvailable()).thenReturn(gitProperties());
        when(buildPropertiesProvider.getIfAvailable()).thenReturn(buildProperties());

        VersionResponse version = createService(frontendProperties()).getVersion();

        assertThat(version.buildTime()).isEqualTo(BUILD_TIME);
        assertThat(version.backend().commitId()).isEqualTo(COMMIT_ID);
        assertThat(version.backend().shortCommitId()).isEqualTo(SHORT_COMMIT_ID);
        assertThat(version.backend().commitTime()).isEqualTo(COMMIT_TIME);
        assertThat(version.backend().branch()).isEqualTo(BRANCH);
        assertThat(version.backend().version()).isEqualTo(ARTIFACT_VERSION);
        assertThat(version.frontend().commitId()).isEqualTo(FRONTEND_COMMIT_ID);
        assertThat(version.frontend().shortCommitId()).isEqualTo(FRONTEND_SHORT_COMMIT_ID);
        assertThat(version.frontend().commitTime()).isEqualTo(FRONTEND_COMMIT_TIME);
        assertThat(version.frontend().branch()).isEqualTo(BRANCH);
        assertThat(version.frontend().version()).isEqualTo(FRONTEND_VERSION);
    }

    @Test
    void getVersion_ShouldReturnEmptyValues_WhenBuildMetadataIsMissing() {
        VersionResponse version = createService(emptyFrontendProperties()).getVersion();

        assertThat(version.buildTime()).isNull();
        assertThat(version.backend()).isNotNull();
        assertThat(version.backend().commitId()).isNull();
        assertThat(version.backend().shortCommitId()).isNull();
        assertThat(version.backend().commitTime()).isNull();
        assertThat(version.backend().branch()).isNull();
        assertThat(version.backend().version()).isNull();
        assertThat(version.frontend()).isNotNull();
        assertThat(version.frontend().commitId()).isNull();
        assertThat(version.frontend().shortCommitId()).isNull();
        assertThat(version.frontend().commitTime()).isNull();
        assertThat(version.frontend().branch()).isNull();
        assertThat(version.frontend().version()).isNull();
    }

    @Test
    void getVersion_ShouldReturnCommitDataOnly_WhenBuildInfoIsMissing() {
        when(gitPropertiesProvider.getIfAvailable()).thenReturn(gitProperties());

        VersionResponse version = createService(emptyFrontendProperties()).getVersion();

        assertThat(version.backend().commitId()).isEqualTo(COMMIT_ID);
        assertThat(version.backend().commitTime()).isEqualTo(COMMIT_TIME);
        assertThat(version.backend().branch()).isEqualTo(BRANCH);
        assertThat(version.backend().version()).isNull();
        assertThat(version.buildTime()).isNull();
    }

    @Test
    void getVersion_ShouldReturnBackendDataOnly_WhenFrontendInfoIsMissing() {
        when(gitPropertiesProvider.getIfAvailable()).thenReturn(gitProperties());
        when(buildPropertiesProvider.getIfAvailable()).thenReturn(buildProperties());

        VersionResponse version = createService(emptyFrontendProperties()).getVersion();

        assertThat(version.backend().commitId()).isEqualTo(COMMIT_ID);
        assertThat(version.backend().version()).isEqualTo(ARTIFACT_VERSION);
        assertThat(version.frontend().commitId()).isNull();
        assertThat(version.frontend().shortCommitId()).isNull();
        assertThat(version.frontend().commitTime()).isNull();
        assertThat(version.frontend().branch()).isNull();
        assertThat(version.frontend().version()).isNull();
    }

    @Test
    void getVersion_ShouldReturnNullCommitTime_WhenFrontendCommitTimeIsNotParsable() {
        VersionResponse version = createService(frontendProperties("not-a-date")).getVersion();

        assertThat(version.frontend().commitId()).isEqualTo(FRONTEND_COMMIT_ID);
        assertThat(version.frontend().version()).isEqualTo(FRONTEND_VERSION);
        assertThat(version.frontend().commitTime()).isNull();
    }

    @Test
    void getVersion_ShouldReturnCommitTimeAsInstant_WhenFrontendCommitTimeHasOffset() {
        VersionResponse version = createService(frontendProperties("2026-08-21T13:15:00+03:00")).getVersion();

        assertThat(version.frontend().commitTime()).isEqualTo(FRONTEND_COMMIT_TIME);
    }

    private VersionServiceImpl createService(FrontendVersionProperties frontendVersionProperties) {
        return new VersionServiceImpl(gitPropertiesProvider, buildPropertiesProvider, frontendVersionProperties);
    }

    private FrontendVersionProperties frontendProperties() {
        return frontendProperties(FRONTEND_COMMIT_TIME.toString());
    }

    private FrontendVersionProperties frontendProperties(String commitTime) {
        return new FrontendVersionProperties(
            FRONTEND_COMMIT_ID,
            FRONTEND_SHORT_COMMIT_ID,
            commitTime,
            BRANCH,
            FRONTEND_VERSION);
    }

    private FrontendVersionProperties emptyFrontendProperties() {
        return new FrontendVersionProperties(null, null, null, null, null);
    }

    private GitProperties gitProperties() {
        Properties entries = new Properties();
        entries.setProperty("branch", BRANCH);
        entries.setProperty("commit.id", COMMIT_ID);
        entries.setProperty("commit.id.abbrev", SHORT_COMMIT_ID);
        entries.setProperty("commit.time", COMMIT_TIME.toString());
        return new GitProperties(entries);
    }

    private BuildProperties buildProperties() {
        Properties entries = new Properties();
        entries.setProperty("group", "com.ita-social-academy");
        entries.setProperty("artifact", "OITAssist");
        entries.setProperty("version", ARTIFACT_VERSION);
        entries.setProperty("time", BUILD_TIME.toString());
        return new BuildProperties(entries);
    }
}
