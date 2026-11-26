package com.itasocialacademy.oitassist.school.controller;

import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import com.itasocialacademy.oitassist.school.dao.dto.request.SchoolSearchRequest;
import com.itasocialacademy.oitassist.school.dao.dto.response.SchoolResponse;
import com.itasocialacademy.oitassist.school.service.interfaces.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
@Tag(name = "School Management V1", description = "Operations related to schools")
public class SchoolController {
    private final SchoolService schoolService;

    @Operation(
        summary = "Search schools by district and text",
        description = "Returns a paginated list of schools in the specified district "
            + "with optional search by school name or settlement.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page of schools retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PageResponse<SchoolResponse>> searchSchools(
        @ParameterObject @Valid SchoolSearchRequest request,
        @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
            schoolService.searchSchools(request.districtCode(), request.search(), pageable)));
    }

    @Operation(
        summary = "Get school by ID",
        description = "Returns details of a specific school by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "School retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "School not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<SchoolResponse> getSchoolById(@PathVariable Long id) {
        return ResponseEntity.ok(schoolService.getSchoolById(id));
    }
}
