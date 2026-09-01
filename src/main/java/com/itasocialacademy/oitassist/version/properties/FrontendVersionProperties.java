package com.itasocialacademy.oitassist.version.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oit.frontend")
public record FrontendVersionProperties(
    String commitId,
    String shortCommitId,
    String commitTime,
    String branch,
    String version) {
    public FrontendVersionProperties {
        commitId = blankToNull(commitId);
        shortCommitId = blankToNull(shortCommitId);
        commitTime = blankToNull(commitTime);
        branch = blankToNull(branch);
        version = blankToNull(version);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
