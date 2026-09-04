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

class SubmissionFilePolicyTest {
    private final SubmissionFilePolicy policy = new SubmissionFilePolicy();

    @Test
    void supports_ShouldReturnTrue_ForSubmissionWithGenericRole() {
        assertTrue(policy.supports(RelatedEntityType.SUBMISSION, FileRole.GENERIC));
    }

    @ParameterizedTest
    @EnumSource(value = FileRole.class, names = "GENERIC", mode = EnumSource.Mode.EXCLUDE)
    void supports_ShouldReturnFalse_ForSubmissionWithAnyOtherThanGenericRole(FileRole role) {
        assertFalse(policy.supports(RelatedEntityType.SUBMISSION, role));
    }

    @Test
    void supports_ShouldReturnFalse_ForNonSubmissionEntity() {
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
