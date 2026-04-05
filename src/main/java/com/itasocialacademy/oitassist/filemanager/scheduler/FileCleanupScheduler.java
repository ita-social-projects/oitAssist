package com.itasocialacademy.oitassist.filemanager.scheduler;

import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {
    private final FileCleanupService cleanupService;

    @Scheduled(cron = "${app.filemanager.cleanup.cron}")
    public void scheduledTask() {
        cleanupService.runFullCleanup();
    }
}