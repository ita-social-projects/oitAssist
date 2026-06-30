package com.itasocialacademy.oitassist.filemanager.validation.policy;

import static org.junit.jupiter.api.Assertions.*;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.util.unit.DataSize;

class TaskSolutionFilePolicyTest {
    private final TaskSolutionFilePolicy policy = new TaskSolutionFilePolicy();

    @Test
    void supports_ShouldReturnTrue_ForTaskWithSolutionRole() {
        assertTrue(policy.supports(RelatedEntityType.TASK, FileRole.SOLUTION));
    }

    @ParameterizedTest
    @EnumSource(value = FileRole.class, names = "SOLUTION", mode = EnumSource.Mode.EXCLUDE)
    void supports_ShouldReturnFalse_ForAnyOtherRole(FileRole role) {
        assertFalse(policy.supports(RelatedEntityType.TASK, role));
    }

    @Test
    void supports_ShouldReturnFalse_ForNonTaskEntity() {
        assertFalse(policy.supports(RelatedEntityType.NEWS, FileRole.SOLUTION));
    }

    @Test
    void getAllowedExtensions_ShouldReturnExpectedExtensions_WhenCalled() {
        Set<AllowedExtension> expected = Set.of(
            AllowedExtension.DOCX,
            AllowedExtension.XLSX,
            AllowedExtension.PPTX,
            AllowedExtension.ACCDB);

        Set<AllowedExtension> actual = policy.getAllowedExtensions();

        assertEquals(expected, actual);
    }

    @Test
    void getMaxFileCount_ShouldReturnFive_WhenCalled() {
        int actual = policy.getMaxFileCount();

        assertEquals(5, actual);
    }

    @Test
    void getMaxFileSize_ShouldReturnFiftyMegabytes_WhenCalled() {
        DataSize actual = policy.getMaxFileSize();

        assertEquals(DataSize.ofMegabytes(50), actual);
    }
}
