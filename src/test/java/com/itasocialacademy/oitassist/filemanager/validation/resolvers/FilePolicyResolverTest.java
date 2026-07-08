package com.itasocialacademy.oitassist.filemanager.validation.resolvers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FilePolicyResolverTest {
    @Mock
    private FilePolicy newsPolicy;

    @Mock
    private FilePolicy taskPolicy;

    @Test
    void resolve_ShouldReturnMatchingPolicy_WhenPolicyExists() {
        // Arrange
        when(newsPolicy.supports(RelatedEntityType.NEWS, FileRole.GENERIC)).thenReturn(true);
        FilePolicyResolver resolver = new FilePolicyResolver(List.of(newsPolicy));

        // Act
        FilePolicy result = resolver.resolve(RelatedEntityType.NEWS, FileRole.GENERIC);

        // Assert
        assertSame(newsPolicy, result);
    }

    @Test
    void resolve_ShouldReturnCorrectPolicy_WhenMultiplePoliciesExist() {
        // Arrange
        when(newsPolicy.supports(RelatedEntityType.TASK, FileRole.SOLUTION)).thenReturn(false);
        when(taskPolicy.supports(RelatedEntityType.TASK, FileRole.SOLUTION)).thenReturn(true);
        FilePolicyResolver resolver = new FilePolicyResolver(List.of(newsPolicy, taskPolicy));

        // Act
        FilePolicy result = resolver.resolve(RelatedEntityType.TASK, FileRole.SOLUTION);

        // Assert
        assertSame(taskPolicy, result);
    }

    @Test
    void resolve_ShouldThrowValidationException_WhenNoPolicyMatchesCombination() {
        // Arrange
        when(newsPolicy.supports(RelatedEntityType.TASK, FileRole.SOLUTION)).thenReturn(false);
        FilePolicyResolver resolver = new FilePolicyResolver(List.of(newsPolicy));

        // Act
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> resolver.resolve(RelatedEntityType.TASK, FileRole.SOLUTION));

        // Assert
        assertAll(
            () -> assertTrue(exception.getMessage().contains("TASK")),
            () -> assertTrue(exception.getMessage().contains("SOLUTION")),
            () -> assertEquals(ErrorCode.FILE_VALIDATION_FAILED, exception.getErrorCode()));
    }

}
