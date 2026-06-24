package com.itasocialacademy.oitassist.filemanager.dto.response;

import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponseDto implements EntityDTO<Long> {
    private Long id;
    private String storageKey;
    private String mimeType;
    private Long size;
    private String url;
}
