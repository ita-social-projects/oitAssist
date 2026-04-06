package com.itasocialacademy.oitassist.filemanager.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app.filemanager.cleanup")
@Validated
@Getter
@Setter
public class FileCleanupConfig {
    /** Grace period for TEMPORARY files in hours. Default: 24. */
    @Positive
    private int orphanHours = 24;

    /** Grace period for SOFT_DELETED files in hours. Default: 720 (30 days). */
    @Positive
    private int expiredHours = 720;

    /**
     * Grace period for rogue file (file without a db record) in hours. Default: 1
     */
    @Positive
    private int rogueGraceHours = 1;

    /** Cron expression for the background job. Default: 03:00 daily. */
    @NotBlank
    private String cron = "0 0 3 * * *";
}