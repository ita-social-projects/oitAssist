package com.itasocialacademy.oitassist.logfile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;
import com.itasocialacademy.oitassist.logfile.dao.LogFileDao;
import com.itasocialacademy.oitassist.logfile.dao.LogFileMetadata;
import com.itasocialacademy.oitassist.logfile.exceptions.LogFileListingException;
import com.itasocialacademy.oitassist.logfile.mapper.LogFileMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class LogFileServiceImplTest {

    private static final int DEFAULT_PAGE_SIZE = 10;

    @Mock
    private LogFileDao logFileDao;

    @Mock
    private LogFileMapper logFileMapper;

    private LogFileServiceImpl logFileService;

    @BeforeEach
    void setUp() {
        logFileService = new LogFileServiceImpl(
            logFileDao,
            logFileMapper);
    }

    @Test
    void shouldReturnFirstPageSortedByLastModifiedDescending() {
        Instant newest =
            Instant.parse("2026-07-22T12:00:00Z");

        Instant middle =
            Instant.parse("2026-07-21T12:00:00Z");

        Instant oldest =
            Instant.parse("2026-07-20T12:00:00Z");

        when(logFileDao.findAll()).thenReturn(
            List.of(
                metadata("old.log", 100, oldest),
                metadata("new.log", 300, newest),
                metadata("middle.log", 200, middle)));

        stubMapper();

        Pageable pageable = PageRequest.of(0, 2);

        PageResponse<LogFileResponse> result =
            logFileService.getAll(pageable);

        assertThat(result.content())
            .extracting(LogFileResponse::fileName)
            .containsExactly(
                "new.log",
                "middle.log");

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldReturnSecondPage() {
        Instant newest =
            Instant.parse("2026-07-22T12:00:00Z");

        when(logFileDao.findAll()).thenReturn(
            List.of(
                metadata(
                    "first.log",
                    100,
                    newest),
                metadata(
                    "second.log",
                    200,
                    newest.minusSeconds(60)),
                metadata(
                    "third.log",
                    300,
                    newest.minusSeconds(120))));

        stubMapper();

        Pageable pageable = PageRequest.of(1, 2);

        PageResponse<LogFileResponse> result =
            logFileService.getAll(pageable);

        assertThat(result.content())
            .extracting(LogFileResponse::fileName)
            .containsExactly("third.log");

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldSortFilesWithSameModificationTimeByFileName() {
        Instant sameModificationTime =
            Instant.parse("2026-07-22T12:00:00Z");

        when(logFileDao.findAll()).thenReturn(
            List.of(
                metadata(
                    "charlie.log",
                    100,
                    sameModificationTime),
                metadata(
                    "Alpha.log",
                    100,
                    sameModificationTime),
                metadata(
                    "bravo.log",
                    100,
                    sameModificationTime)));

        stubMapper();

        Pageable pageable =
            PageRequest.of(0, DEFAULT_PAGE_SIZE);

        PageResponse<LogFileResponse> result =
            logFileService.getAll(pageable);

        assertThat(result.content())
            .extracting(LogFileResponse::fileName)
            .containsExactly(
                "Alpha.log",
                "bravo.log",
                "charlie.log");

        assertThat(result.page()).isZero();
        assertThat(result.size())
            .isEqualTo(DEFAULT_PAGE_SIZE);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyPageWhenNoLogFilesExist() {
        when(logFileDao.findAll())
            .thenReturn(List.of());

        Pageable pageable =
            PageRequest.of(0, DEFAULT_PAGE_SIZE);

        PageResponse<LogFileResponse> result =
            logFileService.getAll(pageable);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isZero();
        assertThat(result.size())
            .isEqualTo(DEFAULT_PAGE_SIZE);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();

        verifyNoInteractions(logFileMapper);
    }

    @Test
    void shouldReturnEmptyContentWhenPageIsOutOfRange() {
        when(logFileDao.findAll()).thenReturn(
            List.of(
                metadata(
                    "app.log",
                    100,
                    Instant.parse(
                        "2026-07-22T12:00:00Z"))));

        Pageable pageable =
            PageRequest.of(10, DEFAULT_PAGE_SIZE);

        PageResponse<LogFileResponse> result =
            logFileService.getAll(pageable);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(10);
        assertThat(result.size())
            .isEqualTo(DEFAULT_PAGE_SIZE);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);

        verifyNoInteractions(logFileMapper);
    }

    @Test
    void shouldPropagateLogFileListingExceptionFromDao() {
        LogFileListingException exception =
            new LogFileListingException();

        when(logFileDao.findAll())
            .thenThrow(exception);

        Pageable pageable =
            PageRequest.of(0, DEFAULT_PAGE_SIZE);

        assertThatThrownBy(
            () -> logFileService.getAll(pageable)).isSameAs(exception);

        verifyNoInteractions(logFileMapper);
    }

    @Test
    void shouldSortBySizeAscending() {
        Instant timestamp =
            Instant.parse("2026-07-22T12:00:00Z");

        when(logFileDao.findAll()).thenReturn(
            List.of(
                metadata("large.log", 300, timestamp),
                metadata("small.log", 100, timestamp),
                metadata("middle.log", 200, timestamp)));

        stubMapper();

        Pageable pageable = PageRequest.of(
            0,
            DEFAULT_PAGE_SIZE,
            Sort.by(Sort.Direction.ASC, "size"));

        PageResponse<LogFileResponse> result =
            logFileService.getAll(pageable);

        assertThat(result.content())
            .extracting(LogFileResponse::fileName)
            .containsExactly(
                "small.log",
                "middle.log",
                "large.log");
    }

    @Test
    void shouldSortByFileNameDescending() {
        Instant timestamp =
            Instant.parse("2026-07-22T12:00:00Z");

        when(logFileDao.findAll()).thenReturn(
            List.of(
                metadata("alpha.log", 100, timestamp),
                metadata("charlie.log", 100, timestamp),
                metadata("bravo.log", 100, timestamp)));

        stubMapper();

        Pageable pageable = PageRequest.of(
            0,
            DEFAULT_PAGE_SIZE,
            Sort.by(Sort.Direction.DESC, "fileName"));

        PageResponse<LogFileResponse> result =
            logFileService.getAll(pageable);

        assertThat(result.content())
            .extracting(LogFileResponse::fileName)
            .containsExactly(
                "charlie.log",
                "bravo.log",
                "alpha.log");
    }

    @Test
    void shouldUseFileNameAsTieBreakerForCustomSort() {
        Instant timestamp =
            Instant.parse("2026-07-22T12:00:00Z");

        when(logFileDao.findAll()).thenReturn(
            List.of(
                metadata("charlie.log", 100, timestamp),
                metadata("alpha.log", 100, timestamp),
                metadata("bravo.log", 100, timestamp)));

        stubMapper();

        Pageable pageable = PageRequest.of(
            0,
            DEFAULT_PAGE_SIZE,
            Sort.by(Sort.Direction.ASC, "size"));

        PageResponse<LogFileResponse> result =
            logFileService.getAll(pageable);

        assertThat(result.content())
            .extracting(LogFileResponse::fileName)
            .containsExactly(
                "alpha.log",
                "bravo.log",
                "charlie.log");
    }

    @Test
    void shouldApplyMultipleSortOrders() {
        Instant oldest =
            Instant.parse("2026-07-20T12:00:00Z");

        Instant newest =
            Instant.parse("2026-07-22T12:00:00Z");

        when(logFileDao.findAll()).thenReturn(
            List.of(
                metadata("third.log", 200, oldest),
                metadata("first.log", 100, newest),
                metadata("second.log", 200, newest)));

        stubMapper();

        Sort sort = Sort.by(
            Sort.Order.desc("size"),
            Sort.Order.desc("lastModified"));

        Pageable pageable =
            PageRequest.of(
                0,
                DEFAULT_PAGE_SIZE,
                sort);

        PageResponse<LogFileResponse> result =
            logFileService.getAll(pageable);

        assertThat(result.content())
            .extracting(LogFileResponse::fileName)
            .containsExactly(
                "second.log",
                "third.log",
                "first.log");
    }

    @Test
    void shouldThrowValidationExceptionForUnsupportedSortProperty() {
        Pageable pageable = PageRequest.of(
            0,
            DEFAULT_PAGE_SIZE,
            Sort.by("unsupported"));

        assertThatThrownBy(
            () -> logFileService.getAll(pageable))
            .isInstanceOf(ValidationException.class);

        verifyNoInteractions(
            logFileDao,
            logFileMapper);
    }

    private void stubMapper() {
        when(
            logFileMapper.toResponse(
                any(LogFileMetadata.class)))
            .thenAnswer(invocation -> {
                LogFileMetadata metadata =
                    invocation.getArgument(0);

                return new LogFileResponse(
                    metadata.fileName(),
                    metadata.size(),
                    metadata.lastModified());
            });
    }

    private static LogFileMetadata metadata(
        String fileName,
        long size,
        Instant lastModified) {
        return new LogFileMetadata(
            fileName,
            size,
            lastModified);
    }
}