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

class NewsFilePolicyTest {
    private final NewsFilePolicy policy = new NewsFilePolicy();

    @Test
    void supports_ShouldReturnTrue_ForNewsWithGenericRole() {
        assertTrue(policy.supports(RelatedEntityType.NEWS, FileRole.GENERIC));
    }

    @ParameterizedTest
    @EnumSource(value = FileRole.class, names = "GENERIC", mode = EnumSource.Mode.EXCLUDE)
    void supports_ShouldReturnFalse_ForNewsWithNonGenericRole(FileRole role) {
        assertFalse(policy.supports(RelatedEntityType.NEWS, role));
    }

    @Test
    void supports_ShouldReturnFalse_ForNonNewsEntity() {
        assertFalse(policy.supports(RelatedEntityType.TASK, FileRole.GENERIC));
    }

    @Test
    void getAllowedExtensions_ShouldReturnExpectedExtensions_WhenCalled() {
        Set<AllowedExtension> expected = Set.of(
            AllowedExtension.JPG,
            AllowedExtension.JPEG,
            AllowedExtension.PNG,
            AllowedExtension.GIF,
            AllowedExtension.WEBP);

        Set<AllowedExtension> actual = policy.getAllowedExtensions();

        assertEquals(expected, actual);
    }

    @Test
    void getMaxFileCount_ShouldReturnTen_WhenCalled() {
        int actual = policy.getMaxFileCount();

        assertEquals(10, actual);
    }

    @Test
    void getMaxFileSize_ShouldReturnTenMegabytes_WhenCalled() {
        DataSize actual = policy.getMaxFileSize();

        assertEquals(DataSize.ofMegabytes(10), actual);
    }
}