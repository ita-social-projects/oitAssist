package com.itasocialacademy.oitassist.competition.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "DTO for changing a sort position Tour within a Stage")
public record ReorderToursRequest(
    @NotEmpty(message = "tourIds must not be empty") List<@NotNull Long> tourIds) {
}
