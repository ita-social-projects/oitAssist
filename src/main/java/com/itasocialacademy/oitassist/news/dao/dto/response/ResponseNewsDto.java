package com.itasocialacademy.oitassist.news.dao.dto.response;

import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
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
public class ResponseNewsDto implements EntityDTO<Long> {
    @NotNull
    private Long id;
    @NotBlank
    @Size(max = 170)
    private String title;
    @NotBlank
    private String content;
    @NotBlank
    private NewsStatus status;
    @NotBlank
    private OffsetDateTime createdAt;
    private OffsetDateTime publishedAt;
}