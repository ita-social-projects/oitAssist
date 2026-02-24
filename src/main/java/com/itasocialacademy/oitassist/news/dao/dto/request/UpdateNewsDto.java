package com.itasocialacademy.oitassist.news.dao.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateNewsDto implements UpdateEntityDTO<Long> {
    @NotNull
    private Long id;
    @NotBlank
    @Size(max = 170)
    private String title;
    @NotBlank
    private String content;
    private boolean publishNow;
}
