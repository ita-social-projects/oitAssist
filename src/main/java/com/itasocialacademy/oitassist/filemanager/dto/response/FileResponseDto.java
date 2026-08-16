package com.itasocialacademy.oitassist.filemanager.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponseDto {
    private Long id;
    private String storageKey;
    private String mimeType;
    private Long size;
    private String url;
}
