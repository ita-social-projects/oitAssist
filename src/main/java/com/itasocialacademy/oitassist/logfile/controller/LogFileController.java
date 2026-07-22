package com.itasocialacademy.oitassist.logfile.controller;

import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;
import com.itasocialacademy.oitassist.logfile.service.LogFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/log-files")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Log Files", description = "Administrative API for application log files\"")
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
        @Parameter(
            description = "Zero-based page number",
            example = "0") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Number of files per page",
            example = "10") @RequestParam(defaultValue = "10") int size) {
        return logFileService.getAll(page, size);
    }
}
