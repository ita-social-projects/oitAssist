package com.itasocialacademy.oitassist.school.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "DTO representing a district response")
@Builder
public record DistrictResponse(
    String code,
    String name,
    String regionCode) {
}
