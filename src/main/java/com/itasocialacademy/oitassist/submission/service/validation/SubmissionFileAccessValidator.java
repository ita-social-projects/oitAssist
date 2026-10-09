package com.itasocialacademy.oitassist.submission.service.validation;

import com.itasocialacademy.oitassist.filemanager.access.FileAccessValidator;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.submission.dao.repository.SubmissionRepository;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmissionFileAccessValidator implements FileAccessValidator {
    private final SubmissionRepository submissionRepository;

    @Override
    public RelatedEntityType getEntityType() {
        return RelatedEntityType.SUBMISSION;
    }

    @Override
    public boolean canAccess(Long submissionId, Long userId, Predicate<String> hasRole) {
        if (hasRole.test("JURY") || hasRole.test("ORG") || hasRole.test("ADMIN")) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        return submissionRepository.findById(submissionId)
            .map(submission -> userId.equals(submission.getSubmittedBy()))
            .orElse(false);
    }
}