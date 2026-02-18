package com.itasocialacademy.oitassist.news.dao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CreateNewsDTO {
    @NotBlank
    @Size(max = 170)
    String title;
    @NotBlank
    String content;
    boolean publishNow;
}
