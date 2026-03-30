package com.itasocialacademy.oitassist.filemanager.dao.dto.response;

import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseFileDto implements EntityDTO<Long> {
    private Long id;
    private String storageKey;
    private String mimeType;
    private Long size;
}
