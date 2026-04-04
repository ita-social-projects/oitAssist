package com.itasocialacademy.oitassist.filemanager.validation.interfaces;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileValidationStrategy {
    RelatedEntityType supports();

    ValidationResult validate(List<MultipartFile> files, FileUploadRequestDto requestDto);
}
