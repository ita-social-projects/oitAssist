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
    /**
     * {@inheritDoc}
     *
     * <p>
     * Applies policy-based rules in order: file count, extension, size, and
     * optional filename constraint, against the already-resolved
     * {@code policy}
     * </p>
     */
    @Override
    public ValidationResult validate(List<MultipartFile> files, FileUploadRequestDto requestDto, FilePolicy policy) {
        List<String> violations = new ArrayList<>();

        validateFileCount(files, policy, violations);
        for (MultipartFile file : files) {
            validateExtension(file, policy, violations);
            validateSize(file, policy, violations);
            validateFileName(file, policy, violations);
        }

        return violations.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(violations);
    }

    /**
     * Validates that the number of uploaded files does not exceed the policy limit.
     *
     * @param files      the files to validate
     * @param policy     the applicable file policy
     * @param violations the list to append violation messages to
     */
    private void validateFileCount(List<MultipartFile> files, FilePolicy policy, List<String> violations) {
        if (files.size() > policy.getMaxFileCount()) {
            violations.add("Upload allows at most %d files, got %d."
                .formatted(policy.getMaxFileCount(), files.size()));
        }
    }

    /**
     * Validates that the file's extension is permitted by the policy.
     *
     * @param file       the file to validate
     * @param policy     the applicable file policy
     * @param violations the list to append violation messages to
     */
    protected void validateExtension(MultipartFile file, FilePolicy policy, List<String> violations) {
        String ext = extractExtension(file.getOriginalFilename());
        if (isExtensionNotAllowed(ext, policy.getAllowedExtensions())) {
            violations.add("File '%s' has unsupported extension. Allowed: %s."
                .formatted(file.getOriginalFilename(), formatAllowed(policy.getAllowedExtensions())));
        }
    }

    /**
     * Validates that the file size does not exceed the policy limit.
     *
     * @param file       the file to validate
     * @param policy     the applicable file policy
     * @param violations the list to append violation messages to
     */
    private void validateSize(MultipartFile file, FilePolicy policy, List<String> violations) {
        if (isFileSizeExceeded(file.getSize(), policy.getMaxFileSize())) {
            violations.add("File '%s' exceeds maximum allowed size of %s."
                .formatted(file.getOriginalFilename(), formatSize(policy.getMaxFileSize())));
        }
    }

    /**
     * Validates the file's name against the required filename specified in the
     * policy, if present. Has no effect when the policy imposes no filename
     * constraint.
     *
     * @param file       the file to validate
     * @param policy     the applicable file policy
     * @param violations the list to append violation messages to
     */
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
