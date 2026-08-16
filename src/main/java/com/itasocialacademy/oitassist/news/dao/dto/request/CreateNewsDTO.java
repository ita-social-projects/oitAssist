package com.itasocialacademy.oitassist.news.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "DTO for creating news")
public class CreateNewsDTO {
    @Schema(
        description = "Title of the news",
        example = "New competition announced",
        maxLength = 170,
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 170)
    private String title;
    @Schema(
        description = "Content of the news",
        example = "We are glad to announce a new competition...",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String content;
    @Schema(
        description = "If true, news will be published immediately. Otherwise saved as draft",
        example = "true")
    private boolean publishNow;
    @Schema(
        description = "IDs of temporarily uploaded files to attach to this news",
        example = "[1, 2, 3]")
    private List<Long> fileIds;
}
