package com.itasocialacademy.oitassist.filemanager.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the file cleanup background services.
 *
 * <p>
 * These properties are mapped from the {@code app.filemanager.cleanup} prefix
 * in the application configuration. They define the grace periods and
 * scheduling for different stages of the file cleanup process.
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "app.filemanager.cleanup")
@Validated
@Getter
@Setter
public class FileCleanupConfig {
    /**
     * Grace period for TEMPORARY files in hours.
     *
     * <p>
     * Files marked as TEMPORARY (uploaded but not yet attached to an entity) will
     * be eligible for hard deletion after this period. Default is 24 hours.
     * </p>
     */
    @Positive
    private int orphanHours = 24;

    /**
     * Grace period for SOFT_DELETED files in hours.
     *
     * <p>
     * Files marked as SOFT_DELETED will be permanently removed from storage after
     * this period. Default is 720 hours (30 days).
     * </p>
     */
    @Positive
    private int expiredHours = 720;

    /**
     * Safety grace period for rogue files in hours.
     *
     * <p>
     * A "rogue" file is a physical file in storage with no corresponding database
     * record. This grace period prevents the deletion of files that were very
     * recently uploaded but haven't been indexed in the database yet. Default is 1
     * hour.
     * </p>
     */
    @Positive
    private int rogueGraceHours = 1;

    /**
     * Cron expression defining the execution schedule for the background cleanup
     * job. Default is {@code 0 0 3 * * *} (every day at 03:00 AM).
     */
    @NotBlank
    private String cron = "0 0 3 * * *";
}