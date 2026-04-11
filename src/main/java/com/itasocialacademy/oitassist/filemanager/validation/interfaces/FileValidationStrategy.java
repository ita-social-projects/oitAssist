package com.itasocialacademy.oitassist.filemanager.validation.interfaces;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileValidationStrategy {
    /**
     * Returns the {@link RelatedEntityType} this strategy applies to.
     *
     * @return the supported entity type
     */
    RelatedEntityType supports();

    /**
     * Validates a list of files against the rules defined for the associated entity
     * type.
     *
     * @param files      the files to validate
     * @param requestDto the upload request metadata
     * @return a {@link ValidationResult} indicating success or listing violations
     */
    ValidationResult validate(List<MultipartFile> files, FileUploadRequestDto requestDto);
}
