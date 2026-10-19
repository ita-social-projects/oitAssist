package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import org.springframework.stereotype.Component;

@Component
public class SubmissionFileValidationStrategy extends AbstractFileValidationStrategy {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(RelatedEntityType entityType, FileRole role) {
        return entityType == RelatedEntityType.SUBMISSION && role == FileRole.GENERIC;
    }
}
