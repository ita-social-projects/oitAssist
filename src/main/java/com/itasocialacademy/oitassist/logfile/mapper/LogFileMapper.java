package com.itasocialacademy.oitassist.logfile.mapper;

import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.dao.LogFileMetadata;
import org.springframework.stereotype.Component;

@Component
public class LogFileMapper {
    public LogFileResponse toResponse(LogFileMetadata metadata) {
        return new LogFileResponse(metadata.filename(), metadata.size(), metadata.lastModified());
    }
}
