package com.itasocialacademy.oitassist.logfile.dao.model;

import org.springframework.core.io.Resource;

public record LogFileDownloadResult(
    String fileName,
    Resource resource) {
}
