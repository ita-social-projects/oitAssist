package com.itasocialacademy.oitassist.school.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "DTO representing a region response")
@Builder
public record RegionResponse(
    String code,
    String name) {
}
