package com.itasocialacademy.oitassist.task.dao.dto.response;

import lombok.Builder;

@Builder
public record TaskResponseDTO(
    Long id,
    String title,
    String description
){
}
