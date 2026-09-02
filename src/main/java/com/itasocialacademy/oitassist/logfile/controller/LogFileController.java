package com.itasocialacademy.oitassist.logfile.controller;

import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;
import com.itasocialacademy.oitassist.logfile.dao.model.LogFileDownloadResult;
import com.itasocialacademy.oitassist.logfile.service.LogFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/log-files")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Log Files", description = "Administrative API for application log files")
public class LogFileController {
    private final LogFileService logFileService;

    public LogFileController(LogFileService logFileService) {
        this.logFileService = logFileService;
    }

    @GetMapping
    @Operation(
        summary = "Get application log files",
        description = """
            Returns a paginated list of application log files.
            Files are sorted by modification date from newest
            to oldest.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of log files retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid page or size parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")})
    public PageResponse<LogFileResponse> getAll(
        @ParameterObject @PageableDefault(
            size = 10,
            sort = "lastModified",
            direction = Sort.Direction.DESC) Pageable pageable) {
        return logFileService.getAll(pageable);
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search log files by name",
        description = """
            Searches application log files by a partial file name match.
            The search is case-insensitive and supports pagination and sorting.
            Access is restricted to administrators.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "Log files matching the specified name were successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid search or sorting parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")})
    public PageResponse<LogFileResponse> searchByName(
        @Parameter(
            description = "Full or partial log file name to search for",
            example = "app") @RequestParam String name,
        @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return logFileService.searchByName(name, pageable);
    }

    @GetMapping("/{fileName}/download")
    @Operation(
        summary = "Download log file",
        description = """
            Downloads a specific application log file
            by its exact file name.
            The file is returned as an attachment.
            Access is restricted to administrators.
            """)
    public ResponseEntity<Resource> downloadFile(
        @Parameter(
            description = "Exact file name of the log file to download",
            example = "application.log") @PathVariable String fileName,
        Authentication authentication) {
        LogFileDownloadResult downloadResult = logFileService.downloadFile(fileName);
        String adminName = authentication.getName();

        ContentDisposition contentDisposition =
            ContentDisposition
                .attachment()
                .filename(
                    downloadResult.fileName(),
                    StandardCharsets.UTF_8)
                .build();

        log.info(
            "Log file download started: fileName={}, admin={}",
            downloadResult.fileName(),
            adminName);
        return ResponseEntity.ok()
            .contentType(
                MediaType.APPLICATION_OCTET_STREAM)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                contentDisposition.toString())
            .body(
                downloadResult.resource());
    }
}
