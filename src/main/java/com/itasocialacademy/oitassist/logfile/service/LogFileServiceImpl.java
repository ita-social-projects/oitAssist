package com.itasocialacademy.oitassist.logfile.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;
import com.itasocialacademy.oitassist.logfile.dao.LogFileDao;
import com.itasocialacademy.oitassist.logfile.dao.LogFileMetadata;
import com.itasocialacademy.oitassist.logfile.mapper.LogFileMapper;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogFileServiceImpl implements LogFileService {
    private static final int MAX_PAGE_SIZE = 100;

    private static final Comparator<LogFileMetadata> DEFAULT_SORT_ORDER =
        Comparator.comparing(LogFileMetadata::lastModified)
            .reversed()
            .thenComparing(LogFileMetadata::filename,
                String.CASE_INSENSITIVE_ORDER);

    private final LogFileDao logFileDao;
    private final LogFileMapper logFileMapper;

    public LogFileServiceImpl(LogFileDao logFileDao, LogFileMapper logFileMapper) {
        this.logFileDao = logFileDao;
        this.logFileMapper = logFileMapper;
    }

    @Override
    public PageResponse<LogFileResponse> getAll(int page, int size) {
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page, size);

        log.debug("Fetching log files with pagination: page={}, size={}", page, size);

        List<LogFileMetadata> sortedFiles = logFileDao.findAll().stream()
            .sorted(DEFAULT_SORT_ORDER)
            .toList();

        List<LogFileMetadata> pageContent = sortedFiles.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .toList();

        Page<LogFileMetadata> metadataPage = new PageImpl<>(pageContent, pageable, sortedFiles.size());

        Page<LogFileResponse> responsePage = metadataPage.map(logFileMapper::toResponse);

        log.debug(
            "Returning {} of {} application log files for page {}",
            responsePage.getNumberOfElements(),
            responsePage.getTotalElements(),
            responsePage.getNumber());

        return new PageResponse<>(
            responsePage.getContent(),
            responsePage.getNumber(),
            responsePage.getSize(),
            responsePage.getTotalElements(),
            responsePage.getTotalPages());
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new ValidationException(
                "Page number must be greater than or equal to 0",
                ErrorCode.INVALID_LOG_FILE_PAGINATION);
        }

        if (size < 1) {
            throw new ValidationException(
                "Page size must be greater than 0",
                ErrorCode.INVALID_LOG_FILE_PAGINATION);
        }

        if (size > MAX_PAGE_SIZE) {
            throw new ValidationException(
                "Size must not exceed " + MAX_PAGE_SIZE,
                ErrorCode.INVALID_LOG_FILE_PAGINATION);
        }
    }
}
