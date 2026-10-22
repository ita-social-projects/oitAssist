package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SubmissionFileValidationStrategyTest {

    private final SubmissionFileValidationStrategy strategy = new SubmissionFileValidationStrategy();

    @Test
    void supports_ShouldReturnTrue_ForSubmissionWithGenericRole() {
        assertTrue(strategy.supports(RelatedEntityType.SUBMISSION, FileRole.GENERIC));
    }

    @ParameterizedTest
    @EnumSource(value = FileRole.class, names = "GENERIC", mode = EnumSource.Mode.EXCLUDE)
    void supports_ShouldReturnFalse_ForSubmissionWithNonGenericRole(FileRole role) {
        assertFalse(strategy.supports(RelatedEntityType.SUBMISSION, role));
    }

    @Test
    void supports_ShouldReturnFalse_ForNonSubmissionEntity() {
        assertFalse(strategy.supports(RelatedEntityType.TASK, FileRole.GENERIC));
        assertFalse(strategy.supports(RelatedEntityType.NEWS, FileRole.GENERIC));
    }
}