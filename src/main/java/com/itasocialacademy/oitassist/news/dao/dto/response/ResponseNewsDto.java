package com.itasocialacademy.oitassist.news.dao.dto.response;

import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "DTO representing news response")
public class ResponseNewsDto implements EntityDTO<Long> {
    @Schema(
        description = "Unique identifier of the news",
        example = "1")
    @NotNull
    private Long id;
    @Schema(
        description = "Title of the news",
        example = "New competition announced",
        maxLength = 170)
    @NotBlank
    @Size(max = 170)
    private String title;
    @Schema(
        description = "Content of the news",
        example = "Full news content text...")
    @NotBlank
    private String content;
    @Schema(
        description = "Current status of the news",
        example = "PUBLISHED",
        allowableValues = {"DRAFT", "PUBLISHED"})
    @NotNull
    private NewsStatus status;
    @Schema(
        description = "Date and time when the news was created",
        example = "2026-02-24T14:30:00Z")
    @NotNull
    private OffsetDateTime createdAt;
    @Schema(
        description = "Date and time when the news was published (null if draft)",
        example = "2026-02-24T15:00:00Z",
        nullable = true)
    private OffsetDateTime publishedAt;
}