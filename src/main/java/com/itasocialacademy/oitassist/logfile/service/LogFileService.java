package com.itasocialacademy.oitassist.logfile.service;

import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;
import com.itasocialacademy.oitassist.logfile.dao.model.LogFileDownloadResult;
import org.springframework.data.domain.Pageable;

public interface LogFileService {
    PageResponse<LogFileResponse> getAll(Pageable pageable);

    PageResponse<LogFileResponse> searchByName(String name, Pageable pageable);

    LogFileDownloadResult downloadFile(String fileName);
}
