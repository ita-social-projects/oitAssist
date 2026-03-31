package com.itasocialacademy.oitassist.filemanager.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequestDto implements CreateEntityDTO<Long> {
    private RelatedEntityType relatedEntityType;
    private Long relatedEntityId;
}
