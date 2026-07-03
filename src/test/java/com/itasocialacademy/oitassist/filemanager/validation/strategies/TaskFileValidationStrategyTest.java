package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TaskFileValidationStrategyTest {

    private final TaskFileValidationStrategy strategy = new TaskFileValidationStrategy();

    @ParameterizedTest
    @EnumSource(value = FileRole.class, names = "GENERIC", mode = EnumSource.Mode.EXCLUDE)
    void supports_ShouldReturnTrue_ForTaskWithAnyNonGenericRole(FileRole role) {
        assertTrue(strategy.supports(RelatedEntityType.TASK, role));
    }

    @Test
    void supports_ShouldReturnFalse_ForTaskWithGenericRole() {
        assertFalse(strategy.supports(RelatedEntityType.TASK, FileRole.GENERIC));
    }

    @Test
    void supports_ShouldReturnFalse_ForNonTaskEntities() {
        assertFalse(strategy.supports(RelatedEntityType.NEWS, FileRole.PROBLEM));
        assertFalse(strategy.supports(RelatedEntityType.SUBMISSION, FileRole.SOLUTION));
    }
}