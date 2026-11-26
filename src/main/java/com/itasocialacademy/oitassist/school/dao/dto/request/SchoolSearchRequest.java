package com.itasocialacademy.oitassist.school.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Query parameters for searching schools")
public record SchoolSearchRequest(
    @Schema(description = "Code of the district (e.g. UA7406)", example = "UA7406") @NotBlank String districtCode,

    @Schema(description = "Search term to match against school name or settlement") @Size(max = 100) String search) {
}
