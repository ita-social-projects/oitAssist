package com.itasocialacademy.oitassist.logfile.controller;

import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;
import com.itasocialacademy.oitassist.logfile.service.LogFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
