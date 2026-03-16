package com.itasocialacademy.oitassist.news.dao.dto.response;

import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "DTO representing news list item response")
public class ResponseNewsListItemDto implements EntityDTO<Long> {
    @Schema(description = "Unique identifier of the news")
    private Long id;
    @Schema(description = "Title of the news")
    private String title;
    @Schema(description = "Short preview of the news")
    private String contentPreview;
    @Schema(description = "Date and time when the news was published")
    private OffsetDateTime publishedAt;
}
