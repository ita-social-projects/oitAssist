package com.itasocialacademy.oitassist.version.controller;

import com.itasocialacademy.oitassist.version.dao.dto.response.VersionResponse;
import com.itasocialacademy.oitassist.version.service.interfaces.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/version")
@Tag(name = "Version", description = "Public API for the build version of the running application")
public class VersionController {
    private final VersionService versionService;

    @GetMapping
    @Operation(
        summary = "Get application build version",
        description = """
            Returns the build version of the running application: the commit the backend
            was built from and the date the artifact was built.
            Values that were not available at build time are returned as null.
            Publicly accessible, no authentication required.
            """)
    @ApiResponse(
        responseCode = "200",
        description = "Build version retrieved successfully",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = VersionResponse.class)))
    public VersionResponse getVersion() {
        return versionService.getVersion();
    }
}
