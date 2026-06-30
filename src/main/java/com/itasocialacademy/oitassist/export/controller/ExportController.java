package com.itasocialacademy.oitassist.export.controller;

import com.itasocialacademy.oitassist.export.dao.dto.ExportData;
import com.itasocialacademy.oitassist.export.dao.enums.ExportFormat;
import com.itasocialacademy.oitassist.export.service.ExporterResolver;
import com.itasocialacademy.oitassist.export.service.interfaces.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Export v1", description = "Operations related to exporting evaluation results")
@RestController
@RequestMapping("/api/v1/export")
public class ExportController {
    private final ExportService exportService;
    private final ExporterResolver exporterResolver;

    public ExportController(ExportService exportService, ExporterResolver exporterResolver) {
        this.exportService = exportService;
        this.exporterResolver = exporterResolver;
    }

    @Operation(
        summary = "Export evaluation results",
        description = "Exports participants' results in the requested format (e.g. Excel)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File generated successfully"),
        @ApiResponse(responseCode = "400", description = "Unsupported export format"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    // @PreAuthorize("hasRole('ORG')")
    @GetMapping
    public ResponseEntity<byte[]> export(
        @RequestParam Long olympiadId,
        @RequestParam(required = false) List<Long> stageIds,
        @RequestParam(required = false) List<Long> tourIds,
        @RequestParam ExportFormat format) {
        ExportData data = exportService.getExportData(olympiadId, stageIds, tourIds);
        byte[] file = exporterResolver.resolve(format).export(data);

        String fileName = data.olympiadName() + "." + format.getExtension();
        ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(fileName, StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .contentType(MediaType.parseMediaType(format.getMimeType()))
            .body(file);
    }
}
