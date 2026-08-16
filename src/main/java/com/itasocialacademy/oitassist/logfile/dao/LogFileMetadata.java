package com.itasocialacademy.oitassist.logfile.dao;

import java.time.Instant;
import java.util.Objects;

public record LogFileMetadata(
    String fileName,
    long size,
    Instant lastModified) {
    public LogFileMetadata {
        Objects.requireNonNull(
            fileName,
            "filename must not be null");
        Objects.requireNonNull(
            lastModified,
            "lastModified must not be null");

        if (fileName.isBlank()) {
            throw new IllegalArgumentException("filename must not be blank");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
    }
}
