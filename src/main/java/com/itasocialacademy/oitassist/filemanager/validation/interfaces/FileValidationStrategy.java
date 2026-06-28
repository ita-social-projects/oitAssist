package com.itasocialacademy.oitassist.filemanager.validation.interfaces;

import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileValidationStrategy {
    /**
     * Indicates whether this strategy handles validation for the given
     * combination of entity type and file role.
     */
    boolean supports(RelatedEntityType entityType, FileRole role);

    /**
     * Validates the uploaded files against the given policy.
     *
     * @param files      the files to validate
     * @param requestDto the upload request
     * @param policy     the policy already resolved for this request's
     *                   entity type and file role
     * @return the validation outcome
     */
    ValidationResult validate(List<MultipartFile> files, FileUploadRequestDto requestDto, FilePolicy policy);
}
