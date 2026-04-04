package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
import com.itasocialacademy.oitassist.filemanager.validation.policy.NewsFilePolicy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class NewsFileValidationStrategy implements FileValidationStrategy {
    private static final NewsFilePolicy POLICY = NewsFilePolicy.INSTANCE;

    @Override
    public RelatedEntityType supports() {
        return RelatedEntityType.NEWS;
    }

    @Override
    public ValidationResult validate(List<MultipartFile> files, FileUploadRequestDto requestDto) {
        List<String> violations = new ArrayList<>();

        if (files.size() > POLICY.getMaxFileCount()) {
            violations.add("News allows at most %d files, got %d."
                .formatted(POLICY.getMaxFileCount(), files.size()));
        }

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            String ext = extractExtension(filename);

            boolean allowed = POLICY.getAllowedExtensions().stream()
                .anyMatch(e -> e.getRawValue().equals(ext));

            if (!allowed) {
                violations.add("File '%s' has unsupported extension. Allowed: %s."
                    .formatted(filename, POLICY.getAllowedExtensions()));
            }
        }

        return violations.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(violations);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
