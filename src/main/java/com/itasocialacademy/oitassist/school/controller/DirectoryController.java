package com.itasocialacademy.oitassist.school.controller;

import com.itasocialacademy.oitassist.school.dao.dto.response.DistrictResponse;
import com.itasocialacademy.oitassist.school.dao.dto.response.RegionResponse;
import com.itasocialacademy.oitassist.school.service.interfaces.DirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Directory Management V1", description = "Operations related to regions and districts")
public class DirectoryController {
    private final DirectoryService directoryService;

    @Operation(
        summary = "Get all regions",
        description = "Returns a list of all regions sorted alphabetically.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of regions retrieved successfully")
    })
    @GetMapping("/regions")
    public ResponseEntity<List<RegionResponse>> getAllRegions() {
        return ResponseEntity.ok(directoryService.getAllRegions());
    }

    @Operation(
        summary = "Get districts by region",
        description = "Returns a list of districts belonging to the specified region code.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of districts retrieved successfully")
    })
    @GetMapping("/districts")
    public ResponseEntity<List<DistrictResponse>> getDistrictsByRegion(
        @Parameter(description = "Code of the region (e.g. UA74)", required = true) @RequestParam String regionCode) {
        return ResponseEntity.ok(directoryService.getDistrictsByRegion(regionCode));
    }
}
