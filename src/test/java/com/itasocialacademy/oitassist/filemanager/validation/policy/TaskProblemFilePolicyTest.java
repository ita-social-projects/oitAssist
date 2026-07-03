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

class TaskProblemFilePolicyTest {
    private final TaskProblemFilePolicy policy = new TaskProblemFilePolicy();

    @Test
    void supports_ShouldReturnTrue_ForTaskWithProblemRole() {
        assertTrue(policy.supports(RelatedEntityType.TASK, FileRole.PROBLEM));
    }

    @ParameterizedTest
    @EnumSource(value = FileRole.class, names = "PROBLEM", mode = EnumSource.Mode.EXCLUDE)
    void supports_ShouldReturnFalse_ForAnyOtherRole(FileRole role) {
        assertFalse(policy.supports(RelatedEntityType.TASK, role));
    }

    @Test
    void supports_ShouldReturnFalse_ForNonTaskEntity() {
        assertFalse(policy.supports(RelatedEntityType.NEWS, FileRole.PROBLEM));
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
    void getMaxFileCount_ShouldReturnTen_WhenCalled() {
        int actual = policy.getMaxFileCount();

        assertEquals(10, actual);
    }

    @Test
    void getMaxFileSize_ShouldReturnFiftyMegabytes_WhenCalled() {
        DataSize actual = policy.getMaxFileSize();

        assertEquals(DataSize.ofMegabytes(50), actual);
    }
}
