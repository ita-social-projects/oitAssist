package com.itasocialacademy.oitassist.logfile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @ParameterizedTest
    @CsvSource({
        "-1, 10",
        "0, 0",
        "0, -1",
        "0, 101"
    })
    void shouldThrowValidationExceptionForInvalidPagination(
        int page,
        int size) {
        Pageable pageable = mock(Pageable.class);

        when(pageable.getPageNumber())
            .thenReturn(page);

        when(pageable.getPageSize())
            .thenReturn(size);

        assertThatThrownBy(
            () -> logFileService.getAll(pageable)).isInstanceOf(ValidationException.class);

        verifyNoInteractions(
            logFileDao,
            logFileMapper);
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

    private void stubMapper() {
        when(
            logFileMapper.toResponse(
                any(LogFileMetadata.class)))
            .thenAnswer(invocation -> {
                LogFileMetadata metadata =
                    invocation.getArgument(0);

                return new LogFileResponse(
                    metadata.filename(),
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