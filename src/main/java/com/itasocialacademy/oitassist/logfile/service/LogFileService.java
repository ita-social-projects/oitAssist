package com.itasocialacademy.oitassist.logfile.service;

import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;

public interface LogFileService {
    PageResponse<LogFileResponse> getAll(int page, int size);
}
