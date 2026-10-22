package com.itasocialacademy.oitassist.news.dao.dto.response;

import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Schema(description = "DTO representing news list item response for admin")
public class ResponseNewsAdminListItemDto {
    @Schema(description = "Unique identifier of the news")
    private Long id;
    @Schema(description = "Title of the news")
    private String title;
    @Schema(description = "Short preview of the news")
    private String contentPreview;
    @Schema(description = "Status of the news")
    private NewsStatus status;
    @Schema(description = "Date and time when the news was created")
    private OffsetDateTime createdAt;
    @Schema(description = "Date and time when the news was published")
    private OffsetDateTime publishedAt;
    @Schema(description = "Date and time when the news was last updated")
    private OffsetDateTime updatedAt;
    @Schema(description = "Date and time when the news was archived")
    private OffsetDateTime archivedAt;
}
