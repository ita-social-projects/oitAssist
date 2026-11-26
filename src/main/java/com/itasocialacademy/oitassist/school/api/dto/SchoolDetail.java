package com.itasocialacademy.oitassist.school.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Details of a School, exposed to other modules via {@code SchoolFacade}.
 */
@Schema(description = "Details of a school exposed to other modules")
@Builder
public record SchoolDetail(
    Long id,
    String edboId,
    String name,
    String shortName,
    String settlement,
    String districtCode,
    String districtName,
    String regionCode,
    String regionName) {
}
