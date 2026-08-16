package com.itasocialacademy.oitassist.filemanager.validation.resolvers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileValidationStrategyResolverTest {

    @Mock
    private FileValidationStrategy newsStrategy;

    @Mock
    private FileValidationStrategy taskStrategy;

    @Test
    void resolve_ShouldReturnMatchingStrategy_WhenStrategyExists() {
        // Arrange
        when(newsStrategy.supports(RelatedEntityType.NEWS, FileRole.GENERIC)).thenReturn(true);
        FileValidationStrategyResolver resolver = new FileValidationStrategyResolver(List.of(newsStrategy));

        // Act
        FileValidationStrategy result = resolver.resolve(RelatedEntityType.NEWS, FileRole.GENERIC);

        // Assert
        assertSame(newsStrategy, result);
    }

    @Test
    void resolve_ShouldReturnCorrectStrategy_WhenMultipleStrategiesExist() {
        // Arrange
        when(newsStrategy.supports(RelatedEntityType.TASK, FileRole.PROBLEM)).thenReturn(false);
        when(taskStrategy.supports(RelatedEntityType.TASK, FileRole.PROBLEM)).thenReturn(true);
        FileValidationStrategyResolver resolver = new FileValidationStrategyResolver(
            List.of(newsStrategy, taskStrategy));

        // Act
        FileValidationStrategy result = resolver.resolve(RelatedEntityType.TASK, FileRole.PROBLEM);

        // Assert
        assertSame(taskStrategy, result);
    }

    @Test
    void resolve_ShouldThrowValidationException_WhenNoStrategyMatchesType() {
        // Arrange
        when(newsStrategy.supports(RelatedEntityType.TASK, FileRole.PROBLEM)).thenReturn(false);
        FileValidationStrategyResolver resolver = new FileValidationStrategyResolver(List.of(newsStrategy));

        // Act
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> resolver.resolve(RelatedEntityType.TASK, FileRole.PROBLEM));

        // Assert
        assertAll(
            () -> assertTrue(exception.getMessage().contains("TASK")),
            () -> assertTrue(exception.getMessage().contains("PROBLEM")),
            () -> assertEquals(ErrorCode.FILE_VALIDATION_FAILED, exception.getErrorCode()));
    }
}
