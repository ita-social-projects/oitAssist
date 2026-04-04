package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.extractExtension;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.formatAllowed;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.isExtensionNotAllowed;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
import com.itasocialacademy.oitassist.filemanager.validation.policy.TaskFilePolicy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class TaskFileValidationStrategy implements FileValidationStrategy {
    private static final TaskFilePolicy POLICY = TaskFilePolicy.INSTANCE;

    @Override
    public RelatedEntityType supports() {
        return RelatedEntityType.TASK;
    }

    @Override
    public ValidationResult validate(List<MultipartFile> files, FileUploadRequestDto requestDto) {
        List<String> violations = new ArrayList<>();

        if (files.size() != POLICY.getMaxFileCount()) {
            violations.add("Task upload requires exactly %d file, got %d."
                .formatted(POLICY.getMaxFileCount(), files.size()));
        }

        for (MultipartFile file : files) {
            String ext = extractExtension(file.getOriginalFilename());
            if (isExtensionNotAllowed(ext, POLICY.getAllowedExtensions())) {
                violations.add("File '%s' has unsupported extension. Allowed for tasks: %s."
                    .formatted(file.getOriginalFilename(), formatAllowed(POLICY.getAllowedExtensions())));
            }
        }

        return violations.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(violations);
    }
}
