package com.itasocialacademy.oitassist.logfile.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;
import com.itasocialacademy.oitassist.logfile.dao.LogFileDao;
import com.itasocialacademy.oitassist.logfile.dao.model.LogFileDownloadResult;
import com.itasocialacademy.oitassist.logfile.dao.model.LogFileMetadata;
import com.itasocialacademy.oitassist.logfile.exceptions.InvalidLogFileNameException;
import com.itasocialacademy.oitassist.logfile.exceptions.LogFileDownloadException;
import com.itasocialacademy.oitassist.logfile.exceptions.LogFileNotFoundException;
import com.itasocialacademy.oitassist.logfile.mapper.LogFileMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

@Slf4j
@Service
public class LogFileServiceImpl implements LogFileService {
    private static final Comparator<LogFileMetadata> FILE_NAME_SORT_ORDER =
        Comparator.comparing(LogFileMetadata::fileName,
            String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<LogFileMetadata> DEFAULT_SORT_ORDER =
        Comparator.comparing(LogFileMetadata::lastModified)
            .reversed()
            .thenComparing(FILE_NAME_SORT_ORDER);

    private final LogFileDao logFileDao;
    private final LogFileMapper logFileMapper;
    private static final Pattern SAFE_FILE_NAME =
        Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._-]{0,254}$");

    public LogFileServiceImpl(LogFileDao logFileDao, LogFileMapper logFileMapper) {
        this.logFileDao = logFileDao;
        this.logFileMapper = logFileMapper;
    }

    @Override
    public PageResponse<LogFileResponse> getAll(Pageable pageable) {
        log.debug("Fetching log files with pagination: page={}, size={}, sort{}",
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort());

        Comparator<LogFileMetadata> sortOrder =
            resolveSortOrder(pageable.getSort());

        return createPageResponse(logFileDao.findAll(), pageable, sortOrder);
    }

    @Override
    public PageResponse<LogFileResponse> searchByName(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(
                "Log file name must not be blank",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
        String searchName = name.trim();

        log.debug(
            "Searching log files by name: '{}', page={}, size={}, sort={}",
            searchName,
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort());
        Comparator<LogFileMetadata> sortOrder =
            resolveSortOrder(pageable.getSort());

        return createPageResponse(logFileDao.findByNameContainingIgnoreCase(searchName), pageable, sortOrder);
    }

    @Override
    public LogFileDownloadResult downloadFile(String fileName) {
        validateDownloadFileName(fileName);

        Path filePath =
            logFileDao.downloadFile(fileName)
                .orElseThrow(
                    () -> new LogFileNotFoundException(fileName));

        Resource resource = createDownloadResource(filePath);

        return new LogFileDownloadResult(
            filePath
                .getFileName()
                .toString(),
            resource);
    }

    private void validateDownloadFileName(String fileName) {
        if (fileName == null
            || fileName.isBlank()
            || !fileName.equals(fileName.trim())
            || fileName.contains("..")
            || !SAFE_FILE_NAME
                .matcher(fileName)
                .matches()) {
            throw new InvalidLogFileNameException();
        }
    }

    private Resource createDownloadResource(Path filePath) {
        try {
            InputStream inputStream =
                Files.newInputStream(
                    filePath,
                    StandardOpenOption.READ,
                    LinkOption.NOFOLLOW_LINKS);

            return new InputStreamResource(inputStream, "Log file " + filePath.getFileName());
        } catch (NoSuchFileException _) {
            throw new LogFileNotFoundException(filePath.getFileName().toString());
        } catch (IOException | SecurityException exception) {
            log.error("Failed to open log file for download: {}", filePath, exception);

            throw new LogFileDownloadException();
        }
    }

    private PageResponse<LogFileResponse> createPageResponse(List<LogFileMetadata> files,
        Pageable pageable, Comparator<LogFileMetadata> sortOrder) {
        List<LogFileMetadata> sortedFiles = files.stream()
            .sorted(sortOrder)
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

    private Comparator<LogFileMetadata> resolveSortOrder(Sort sort) {
        Comparator<LogFileMetadata> result = null;

        for (Sort.Order order : sort) {
            Comparator<LogFileMetadata> comparator =
                comparatorFor(order.getProperty());

            if (order.isDescending()) {
                comparator = comparator.reversed();
            }

            result = result == null
                ? comparator
                : result.thenComparing(comparator);
        }

        if (result == null) {
            return DEFAULT_SORT_ORDER;
        }

        if (sort.getOrderFor("fileName") == null) {
            result = result.thenComparing(FILE_NAME_SORT_ORDER);
        }

        return result;
    }

    private Comparator<LogFileMetadata> comparatorFor(String property) {
        return switch (property) {
            case "fileName" -> FILE_NAME_SORT_ORDER;
            case "size" -> Comparator.comparingLong(LogFileMetadata::size);
            case "lastModified" -> Comparator.comparing(LogFileMetadata::lastModified);
            default -> throw new ValidationException(
                "Unsupported sort property: " + property,
                ErrorCode.INVALID_LOG_FILE_SORT);
        };
    }
}
