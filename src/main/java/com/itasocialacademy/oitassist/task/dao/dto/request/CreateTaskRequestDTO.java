package com.itasocialacademy.oitassist.task.dao.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateTaskRequestDTO(
    @NotBlank String title,
    String description,
    List<Long> fileIds
) {
}
