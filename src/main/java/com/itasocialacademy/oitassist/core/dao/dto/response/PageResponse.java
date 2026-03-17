package com.itasocialacademy.oitassist.core.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Generic paginated response")
public class PageResponse<T> {
    @Schema(description = "List of returned elements")
    private List<T> content;
    @Schema(description = "Current page number (0-based)", example = "0")
    private int pageNumber;
    @Schema(description = "Number of elements per page", example = "10")
    private int pageSize;
    @Schema(description = "Total number of pages", example = "6")
    private int totalPages;
    @Schema(description = "Total number of elements", example = "52")
    private long totalElements;
    @Schema(description = "Indicates if this is the first page", example = "true")
    private boolean first;
    @Schema(description = "Indicates if this is the last page", example = "false")
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.isFirst(),
            page.isLast());
    }
}
