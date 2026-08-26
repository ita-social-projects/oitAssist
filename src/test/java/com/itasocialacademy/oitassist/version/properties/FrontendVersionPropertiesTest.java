package com.itasocialacademy.oitassist.version.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FrontendVersionPropertiesTest {

    private static final String COMMIT_ID = "9f1c2ab7d4e58306b1c0f2a4d7e93b5c8a6d1e42";
    private static final String SHORT_COMMIT_ID = "9f1c2ab";
    private static final String COMMIT_TIME = "2026-08-21T10:15:00Z";
    private static final String BRANCH = "dev";
    private static final String VERSION = "1.4.2";

    @Test
    void constructor_ShouldReplaceValuesWithNull_WhenTheyAreBlank() {
        FrontendVersionProperties properties = new FrontendVersionProperties("", "   ", "", "", "");

        assertThat(properties.commitId()).isNull();
        assertThat(properties.shortCommitId()).isNull();
        assertThat(properties.commitTime()).isNull();
        assertThat(properties.branch()).isNull();
        assertThat(properties.version()).isNull();
    }

    @Test
    void constructor_ShouldKeepValues_WhenTheyArePresent() {
        FrontendVersionProperties properties = new FrontendVersionProperties(
            COMMIT_ID, SHORT_COMMIT_ID, COMMIT_TIME, BRANCH, VERSION);

        assertThat(properties.commitId()).isEqualTo(COMMIT_ID);
        assertThat(properties.shortCommitId()).isEqualTo(SHORT_COMMIT_ID);
        assertThat(properties.commitTime()).isEqualTo(COMMIT_TIME);
        assertThat(properties.branch()).isEqualTo(BRANCH);
        assertThat(properties.version()).isEqualTo(VERSION);
    }

    @Test
    void constructor_ShouldStripValues_WhenTheyAreSurroundedByWhitespace() {
        FrontendVersionProperties properties = new FrontendVersionProperties(
            " " + COMMIT_ID + " ",
            " " + SHORT_COMMIT_ID + " ",
            " " + COMMIT_TIME + " ",
            " " + BRANCH + " ",
            " " + VERSION + " ");

        assertThat(properties.commitId()).isEqualTo(COMMIT_ID);
        assertThat(properties.shortCommitId()).isEqualTo(SHORT_COMMIT_ID);
        assertThat(properties.commitTime()).isEqualTo(COMMIT_TIME);
        assertThat(properties.branch()).isEqualTo(BRANCH);
        assertThat(properties.version()).isEqualTo(VERSION);
    }
}
