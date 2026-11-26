package com.itasocialacademy.oitassist.school.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "DTO representing a school response")
@Builder
public record SchoolResponse(
    Long id,
    String name,
    String shortName,
    String settlement,
    String districtCode) {
}
