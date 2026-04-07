package com.itasocialacademy.oitassist.filemanager.scheduler;

import com.itasocialacademy.oitassist.filemanager.config.FileCleanupConfig;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class FileCleanupScheduler implements SchedulingConfigurer {
    private final FileCleanupService cleanupService;
    private final FileCleanupConfig cleanupConfig;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addCronTask(
            this::scheduledTask,
            cleanupConfig.getCron());
    }

    public void scheduledTask() {
        log.info("Scheduled file cleanup triggered.");
        cleanupService.runFullCleanup();
    }
}