package com.itasocialacademy.oitassist.school.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Details of a Region, exposed to other modules via {@code SchoolFacade}.
 */
@Schema(description = "Details of a region exposed to other modules")
@Builder
public record RegionDetail(
    String code,
    String name) {
}
