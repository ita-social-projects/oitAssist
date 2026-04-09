package com.itasocialacademy.oitassist.filemanager.providers.resolver;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.UnsupportedStorageException;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageProviderResolverTest {

    @Mock
    private StorageProvider localProvider;

    @Mock
    private StorageProvider sharepointProvider;

    @Test
    void resolve_ShouldReturnMatchingProvider_WhenProviderSupportsType() {
        when(localProvider.supports(StorageProviderType.LOCAL)).thenReturn(true);
        StorageProviderResolver resolver = new StorageProviderResolver(List.of(localProvider));

        StorageProvider result = resolver.resolve(StorageProviderType.LOCAL);

        assertSame(localProvider, result);
    }

    @Test
    void resolve_ShouldReturnCorrectProvider_WhenMultipleProvidersExist() {
        when(localProvider.supports(StorageProviderType.SHAREPOINT)).thenReturn(false);
        when(sharepointProvider.supports(StorageProviderType.SHAREPOINT)).thenReturn(true);
        StorageProviderResolver resolver = new StorageProviderResolver(
            List.of(localProvider, sharepointProvider));

        StorageProvider result = resolver.resolve(StorageProviderType.SHAREPOINT);

        assertSame(sharepointProvider, result);
    }

    @Test
    void resolve_ShouldThrowUnsupportedStorageException_WhenNoProviderSupportsType() {
        when(localProvider.supports(StorageProviderType.SHAREPOINT)).thenReturn(false);
        StorageProviderResolver resolver = new StorageProviderResolver(List.of(localProvider));

        UnsupportedStorageException exception = assertThrows(
            UnsupportedStorageException.class,
            () -> resolver.resolve(StorageProviderType.SHAREPOINT));

        assertAll(
            () -> assertTrue(exception.getMessage().contains("SHAREPOINT")),
            () -> assertEquals(ErrorCode.PROVIDER_NOT_SUPPORTED, exception.getErrorCode()));
    }

    @Test
    void resolveDefault_ShouldReturnProvider_WhenDefaultProviderIsConfigured() {
        when(localProvider.supports(StorageProviderType.LOCAL)).thenReturn(true);
        StorageProviderResolver resolver = new StorageProviderResolver(List.of(localProvider));
        ReflectionTestUtils.setField(resolver, "defaultProvider", StorageProviderType.LOCAL);

        StorageProvider result = resolver.resolveDefault();

        assertSame(localProvider, result);
    }

    @Test
    void resolveDefault_ShouldThrowUnsupportedStorageException_WhenDefaultProviderHasNoMatchingProvider() {
        when(localProvider.supports(StorageProviderType.SHAREPOINT)).thenReturn(false);
        StorageProviderResolver resolver = new StorageProviderResolver(List.of(localProvider));
        ReflectionTestUtils.setField(resolver, "defaultProvider", StorageProviderType.SHAREPOINT);

        assertThrows(
            UnsupportedStorageException.class,
            resolver::resolveDefault);
    }
}
