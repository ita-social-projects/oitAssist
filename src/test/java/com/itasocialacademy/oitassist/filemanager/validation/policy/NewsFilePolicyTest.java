package com.itasocialacademy.oitassist.filemanager.validation.policy;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NewsFilePolicyTest {

    private final NewsFilePolicy policy = NewsFilePolicy.INSTANCE;

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