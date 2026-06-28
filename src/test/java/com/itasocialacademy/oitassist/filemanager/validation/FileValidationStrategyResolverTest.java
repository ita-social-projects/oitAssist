package com.itasocialacademy.oitassist.filemanager.validation;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import com.itasocialacademy.oitassist.filemanager.validation.resolvers.FileValidationStrategyResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileValidationStrategyResolverTest {

    @Mock
    private FileValidationStrategy newsStrategy;

    @Mock
    private FileValidationStrategy taskStrategy;

    @Test
    void resolve_ShouldReturnMatchingStrategy_WhenStrategyExists() {
        // Arrange
        when(newsStrategy.supports()).thenReturn(RelatedEntityType.NEWS);
        FileValidationStrategyResolver resolver = new FileValidationStrategyResolver(List.of(newsStrategy));

        // Act
        FileValidationStrategy result = resolver.resolve(RelatedEntityType.NEWS);

        // Assert
        assertSame(newsStrategy, result);
    }

    @Test
    void resolve_ShouldReturnCorrectStrategy_WhenMultipleStrategiesExist() {
        // Arrange
        when(newsStrategy.supports()).thenReturn(RelatedEntityType.NEWS);
        when(taskStrategy.supports()).thenReturn(RelatedEntityType.TASK);
        FileValidationStrategyResolver resolver = new FileValidationStrategyResolver(
            List.of(newsStrategy, taskStrategy));

        // Act
        FileValidationStrategy result = resolver.resolve(RelatedEntityType.TASK);

        // Assert
        assertSame(taskStrategy, result);
    }

    @Test
    void resolve_ShouldThrowValidationException_WhenNoStrategyMatchesType() {
        // Arrange
        when(newsStrategy.supports()).thenReturn(RelatedEntityType.NEWS);
        FileValidationStrategyResolver resolver = new FileValidationStrategyResolver(List.of(newsStrategy));

        // Act
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> resolver.resolve(RelatedEntityType.TASK));

        // Assert
        assertAll(
            () -> assertTrue(exception.getMessage().contains("TASK")),
            () -> assertEquals(ErrorCode.FILE_VALIDATION_FAILED, exception.getErrorCode()));
    }
}
