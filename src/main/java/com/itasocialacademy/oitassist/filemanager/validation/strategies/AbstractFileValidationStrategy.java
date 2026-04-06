package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.extractExtension;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.extractNameWithoutExtension;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.formatAllowed;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.formatSize;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.isExtensionNotAllowed;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.isFileSizeExceeded;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public abstract class AbstractFileValidationStrategy implements FileValidationStrategy {
    protected abstract FilePolicy resolvePolicy(Long relatedEntityId);

    @Override
    public ValidationResult validate(List<MultipartFile> files, FileUploadRequestDto requestDto) {
        FilePolicy policy = resolvePolicy(requestDto.getRelatedEntityId());
        List<String> violations = new ArrayList<>();

        validateFileCount(files, policy, violations);
        for (MultipartFile file : files) {
            validateExtension(file, policy, violations);
            validateSize(file, policy, violations);
            validateFileName(file, policy, violations);
        }

        return violations.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(violations);
    }

    private void validateFileCount(List<MultipartFile> files, FilePolicy policy, List<String> violations) {
        if (files.size() > policy.getMaxFileCount()) {
            violations.add("Upload allows at most %d files, got %d."
                .formatted(policy.getMaxFileCount(), files.size()));
        }
    }

    protected void validateExtension(MultipartFile file, FilePolicy policy, List<String> violations) {
        String ext = extractExtension(file.getOriginalFilename());
        if (isExtensionNotAllowed(ext, policy.getAllowedExtensions())) {
            violations.add("File '%s' has unsupported extension. Allowed: %s."
                .formatted(file.getOriginalFilename(), formatAllowed(policy.getAllowedExtensions())));
        }
    }

    private void validateSize(MultipartFile file, FilePolicy policy, List<String> violations) {
        if (isFileSizeExceeded(file.getSize(), policy.getMaxFileSize())) {
            violations.add("File '%s' exceeds maximum allowed size of %s."
                .formatted(file.getOriginalFilename(), formatSize(policy.getMaxFileSize())));
        }
    }

    private void validateFileName(MultipartFile file, FilePolicy policy, List<String> violations) {
        policy.getRequiredFileName().ifPresent(required -> {
            String nameWithoutExt = extractNameWithoutExtension(file.getOriginalFilename());
            if (!nameWithoutExt.equals(required)) {
                violations.add("File name must be '%s', got '%s'."
                    .formatted(required, nameWithoutExt));
            }
        });
    }
}
