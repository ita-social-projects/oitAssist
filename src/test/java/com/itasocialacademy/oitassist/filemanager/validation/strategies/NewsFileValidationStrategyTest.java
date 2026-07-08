package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NewsFileValidationStrategyTest {

    private final NewsFileValidationStrategy strategy = new NewsFileValidationStrategy();

    @Test
    void supports_ShouldReturnTrue_ForNewsWithGenericRole() {
        assertTrue(strategy.supports(RelatedEntityType.NEWS, FileRole.GENERIC));
    }

    @ParameterizedTest
    @EnumSource(value = FileRole.class, names = "GENERIC", mode = EnumSource.Mode.EXCLUDE)
    void supports_ShouldReturnFalse_ForNewsWithNonGenericRole(FileRole role) {
        assertFalse(strategy.supports(RelatedEntityType.NEWS, role));
    }

    @Test
    void supports_ShouldReturnFalse_ForNonNewsEntity() {
        assertFalse(strategy.supports(RelatedEntityType.TASK, FileRole.GENERIC));
    }
}
