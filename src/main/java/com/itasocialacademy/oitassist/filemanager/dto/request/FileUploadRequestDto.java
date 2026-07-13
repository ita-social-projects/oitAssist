package com.itasocialacademy.oitassist.filemanager.dto.request;

import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUploadRequestDto {
    @NotNull
    private RelatedEntityType relatedEntityType;
    private Long relatedEntityId;

    @Builder.Default
    @NotNull
    private FileRole fileRole = FileRole.GENERIC;
}
