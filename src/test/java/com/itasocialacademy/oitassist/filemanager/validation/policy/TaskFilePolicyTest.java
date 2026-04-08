package com.itasocialacademy.oitassist.filemanager.validation.policy;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TaskFilePolicyTest {

    private final TaskFilePolicy policy = TaskFilePolicy.INSTANCE;

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
    void getMaxFileCount_ShouldReturnOne_WhenCalled() {
        int actual = policy.getMaxFileCount();

        assertEquals(1, actual);
    }

    @Test
    void getMaxFileSize_ShouldReturnFiftyMegabytes_WhenCalled() {
        DataSize actual = policy.getMaxFileSize();

        assertEquals(DataSize.ofMegabytes(50), actual);
    }
}