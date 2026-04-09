package com.itasocialacademy.oitassist.filemanager.scheduler;

import com.itasocialacademy.oitassist.filemanager.config.FileCleanupConfig;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Scheduler configuration responsible for managing background file cleanup
 * tasks.
 *
 * <p>
 * This component implements {@link SchedulingConfigurer} to allow dynamic cron
 * expression resolution from the application configuration. It orchestrates the
 * execution of the {@link FileCleanupService} to maintain storage integrity.
 * </p>
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class FileCleanupScheduler implements SchedulingConfigurer {
    private final FileCleanupService cleanupService;
    private final FileCleanupConfig cleanupConfig;

    /**
     * Configures the scheduled tasks by registering a cron-based task. The cron
     * expression is retrieved dynamically from {@link FileCleanupConfig}.
     *
     * @param registrar the registrar used to configure scheduled tasks.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addCronTask(
            this::scheduledTask,
            cleanupConfig.getCron());
    }

    /**
     * Wrapper method triggered by the scheduler. Delegates the core cleanup logic
     * to the {@link FileCleanupService}.
     */
    public void scheduledTask() {
        log.info("Scheduled file cleanup triggered.");
        cleanupService.runFullCleanup();
    }
}