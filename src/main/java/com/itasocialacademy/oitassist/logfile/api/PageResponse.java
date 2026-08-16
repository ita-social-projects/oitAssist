package com.itasocialacademy.oitassist.logfile.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Paginated response")
public record PageResponse<T>(

    List<T> content,

    @Schema(
        description = "Current zero-based page number",
        example = "0") int page,

    @Schema(
        description = "Requested number of elements per page",
        example = "10") int size,

    @Schema(
        description = "Total number of available elements",
        example = "15") long totalElements,

    @Schema(
        description = "Total number of pages",
        example = "3") int totalPages) {
    public PageResponse {
        Objects.requireNonNull(content, "'content' must not be null");
        content = List.copyOf(content);
    }
}
