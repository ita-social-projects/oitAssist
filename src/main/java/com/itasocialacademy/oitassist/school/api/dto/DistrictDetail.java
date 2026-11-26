package com.itasocialacademy.oitassist.school.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Details of a District, exposed to other modules via {@code SchoolFacade}.
 */
@Schema(description = "Details of a district exposed to other modules")
@Builder
public record DistrictDetail(
    String code,
    String name,
    String regionCode) {
}
