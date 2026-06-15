package com.itasocialacademy.oitassist.news.dao.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "DTO for updating existing news")
public class UpdateNewsDto implements UpdateEntityDTO<Long> {
    @Schema(
        description = "Unique identifier of the news",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;
    @Schema(
        description = "Updated title of the news",
        example = "Updated competition announcement",
        maxLength = 170,
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 170)
    private String title;
    @Schema(
        description = "Updated content of the news",
        example = "Updated content text...",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String content;
    @Schema(
        description = "If true, news will be published immediately",
        example = "true")
    private boolean publishNow;
    @Schema(
        description = "IDs of uploaded files",
        example = "[1, 2, 3]")
    private List<Long> fileIds;
    @Schema(
        description = "IDs of files removed from content")
    private List<Long> removedFileIds;
}
