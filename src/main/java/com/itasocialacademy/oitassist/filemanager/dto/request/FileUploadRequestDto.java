package com.itasocialacademy.oitassist.filemanager.dto.request;

import com.itasocialacademy.oitassist.filemanager.api.dto.StorageContext;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Metadata accompanying a file upload request. Specifies the target entity,
 * file role, and optional {@link StorageContext} for hierarchical path
 * construction.
 *
 * <p>
 * When {@code storageContext} is {@code null}, files are stored in a flat
 * directory named after the entity type (legacy behavior). When provided, files
 * are stored in a human-readable directory hierarchy derived from entity
 * titles.
 * </p>
 */
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

    private StorageContext storageContext;
}
