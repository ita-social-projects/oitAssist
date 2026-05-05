package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.exceptions.InvalidFilePathException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileDeleteException;
import com.itasocialacademy.oitassist.filemanager.properties.GraphProperties;
import com.microsoft.graph.drives.DrivesRequestBuilder;
import com.microsoft.graph.drives.item.DriveItemRequestBuilder;
import com.microsoft.graph.drives.item.items.ItemsRequestBuilder;
import com.microsoft.graph.drives.item.items.item.DriveItemItemRequestBuilder;
import com.microsoft.graph.drives.item.items.item.content.ContentRequestBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharePointStorageProviderTest {

    private SharePointStorageProvider sharePointStorageProvider;

    @Mock
    private GraphServiceClient graphServiceClientMock;

    @Mock
    private GraphProperties graphPropertiesMock;

    @BeforeEach
    void setUp() {
        sharePointStorageProvider = new SharePointStorageProvider(graphServiceClientMock, graphPropertiesMock);
    }

    //
    // getType() SECTION TESTS
    //

    @Test
    void getType_ShouldReturnSharePoint() {
        assertThat(sharePointStorageProvider.getType()).isEqualTo(StorageProviderType.SHAREPOINT);
    }

    //
    // upload() SECTION TESTS
    //

    @Test
    void upload_ShouldReturnStorageKeyWithJustFileName_WhenPathIsNull() {
        String fileName = "document.pdf";
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());

        mockGraphClientChain();

        String result = sharePointStorageProvider.upload(inputStream, fileName, null);

        assertThat(result).isEqualTo(fileName);
    }

    @Test
    void upload_ShouldReturnStorageKeyWithJustFileName_WhenPathIsBlank() {
        String fileName = "report.xlsx";
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());

        mockGraphClientChain();

        String result = sharePointStorageProvider.upload(inputStream, fileName, "   ");

        assertThat(result).isEqualTo(fileName);
    }

    @Test
    void upload_ShouldReturnStorageKeyWithPathAndFileName_WhenPathIsProvided() {
        String fileName = "photo.jpg";
        String path = "images/gallery";
        InputStream inputStream = new ByteArrayInputStream("image-data".getBytes());

        mockGraphClientChain();

        String result = sharePointStorageProvider.upload(inputStream, fileName, path);

        assertThat(result).isEqualTo("images/gallery/photo.jpg");
    }

    @Test
    void upload_ShouldThrowFileUploadException_WhenPathContainsDoubleDotsTraversal() {
        String fileName = "malicious.exe";
        String maliciousPath = "../../etc";
        InputStream inputStream = new ByteArrayInputStream("data".getBytes());

        assertThatThrownBy(() -> sharePointStorageProvider.upload(inputStream, fileName, maliciousPath))
            .isInstanceOf(FileUploadException.class)
            .hasMessageContaining("Invalid path");
    }

    @Test
    void upload_ShouldThrowFileUploadException_WhenGraphApiThrowsException() {
        String fileName = "document.docx";
        String path = "files";
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());

        mockGraphClientChainToThrow(new RuntimeException("Network error"));

        assertThatThrownBy(() -> sharePointStorageProvider.upload(inputStream, fileName, path))
            .isInstanceOf(FileUploadException.class)
            .hasMessageContaining("Failed to upload file to SharePoint")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    //
    // deletePhysical() SECTION TESTS
    //

    @Test
    void deletePhysical_ShouldThrowException_WhenKeyIsNull() {
        InvalidFilePathException exception = assertThrows(
            InvalidFilePathException.class,
            () -> sharePointStorageProvider.deletePhysical(null));

        assertThat(exception).hasMessageContaining("blank storage key");
    }

    @Test
    void deletePhysical_ShouldThrowException_WhenKeyIsBlank() {
        InvalidFilePathException exception = assertThrows(
            InvalidFilePathException.class,
            () -> sharePointStorageProvider.deletePhysical("   "));

        assertThat(exception).hasMessageContaining("blank storage key");
    }

    @Test
    void deletePhysical_ShouldDeleteSuccessfully_WhenStorageKeyIsValid() {
        String storageKey = "images/photo.jpg";

        mockDeleteGraphClientChain();

        assertThatNoException()
            .isThrownBy(() -> sharePointStorageProvider.deletePhysical(storageKey));

        verify(graphServiceClientMock.drives()
            .byDriveId(anyString())
            .items()
            .byDriveItemId(anyString()))
            .delete();
    }

    @Test
    void deletePhysical_ShouldReturnSilently_WhenFileNotFoundInSharePoint() {
        String storageKey = "missing/file.pdf";

        ApiException notFound = mock(ApiException.class);
        when(notFound.getResponseStatusCode()).thenReturn(404);

        mockDeleteGraphClientChainToThrow(notFound);

        assertThatNoException()
            .isThrownBy(() -> sharePointStorageProvider.deletePhysical(storageKey));
    }

    @Test
    void deletePhysical_ShouldThrowFileDeleteException_WhenApiExceptionIsNot404() {
        String storageKey = "documents/report.docx";

        ApiException serverError = mock(ApiException.class);
        when(serverError.getResponseStatusCode()).thenReturn(500);

        mockDeleteGraphClientChainToThrow(serverError);

        assertThatThrownBy(() -> sharePointStorageProvider.deletePhysical(storageKey))
            .isInstanceOf(FileDeleteException.class)
            .hasMessageContaining("Failed to delete file from SharePoint")
            .hasCause(serverError);
    }

    @Test
    void deletePhysical_ShouldThrowFileDeleteException_WhenUnexpectedExceptionOccurs() {
        String storageKey = "docs/contract.pdf";

        mockDeleteGraphClientChainToThrow(new RuntimeException("Unexpected failure"));

        assertThatThrownBy(() -> sharePointStorageProvider.deletePhysical(storageKey))
            .isInstanceOf(FileDeleteException.class)
            .hasMessageContaining("Failed to delete file from SharePoint");
    }

    // HELPER METHODS
    //

    /**
     * Mocks the Graph API chain to return successfully without exceptions. Sets up
     * the entire fluent chain: drives() -> byDriveId() -> items() ->
     * byDriveItemId() -> content() -> put()
     */
    private void mockGraphClientChain() {
        when(graphPropertiesMock.getDriveId()).thenReturn("test-drive-id");
        var drivesRequestBuilder = mock(DrivesRequestBuilder.class);
        var driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
        var itemsRequestBuilder = mock(ItemsRequestBuilder.class);
        var itemRequestBuilder = mock(DriveItemItemRequestBuilder.class);
        var contentRequestBuilder = mock(ContentRequestBuilder.class);

        when(graphServiceClientMock.drives()).thenReturn(drivesRequestBuilder);
        when(drivesRequestBuilder.byDriveId(anyString())).thenReturn(driveItemRequestBuilder);
        when(driveItemRequestBuilder.items()).thenReturn(itemsRequestBuilder);
        when(itemsRequestBuilder.byDriveItemId(anyString())).thenReturn(itemRequestBuilder);
        when(itemRequestBuilder.content()).thenReturn(contentRequestBuilder);
        when(contentRequestBuilder.put(any(InputStream.class))).thenReturn(null);
    }

    /**
     * Mocks the Graph API chain to throw an exception when the content().put() is
     * called.
     *
     * @param exception the exception to throw
     */
    private void mockGraphClientChainToThrow(Exception exception) {
        when(graphPropertiesMock.getDriveId()).thenReturn("test-drive-id");
        var drivesRequestBuilder = mock(DrivesRequestBuilder.class);
        var driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
        var itemsRequestBuilder = mock(ItemsRequestBuilder.class);
        var itemRequestBuilder = mock(DriveItemItemRequestBuilder.class);
        var contentRequestBuilder = mock(ContentRequestBuilder.class);

        when(graphServiceClientMock.drives()).thenReturn(drivesRequestBuilder);
        when(drivesRequestBuilder.byDriveId(anyString())).thenReturn(driveItemRequestBuilder);
        when(driveItemRequestBuilder.items()).thenReturn(itemsRequestBuilder);
        when(itemsRequestBuilder.byDriveItemId(anyString())).thenReturn(itemRequestBuilder);
        when(itemRequestBuilder.content()).thenReturn(contentRequestBuilder);
        when(contentRequestBuilder.put(any(InputStream.class))).thenThrow(exception);
    }

    /**
     * Mocks the Graph API delete chain to complete successfully. Sets up: drives()
     * -> byDriveId() -> items() -> byDriveItemId() -> delete()
     */
    private void mockDeleteGraphClientChain() {
        when(graphPropertiesMock.getDriveId()).thenReturn("test-drive-id");
        var drivesRequestBuilder = mock(DrivesRequestBuilder.class);
        var driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
        var itemsRequestBuilder = mock(ItemsRequestBuilder.class);
        var itemRequestBuilder = mock(DriveItemItemRequestBuilder.class);

        when(graphServiceClientMock.drives()).thenReturn(drivesRequestBuilder);
        when(drivesRequestBuilder.byDriveId(anyString())).thenReturn(driveItemRequestBuilder);
        when(driveItemRequestBuilder.items()).thenReturn(itemsRequestBuilder);
        when(itemsRequestBuilder.byDriveItemId(anyString())).thenReturn(itemRequestBuilder);
        doNothing().when(itemRequestBuilder).delete();
    }

    /**
     * Mocks the Graph API delete chain to throw an exception when delete() is
     * called.
     *
     * @param exception the exception to throw
     */
    private void mockDeleteGraphClientChainToThrow(Exception exception) {
        when(graphPropertiesMock.getDriveId()).thenReturn("test-drive-id");
        var drivesRequestBuilder = mock(DrivesRequestBuilder.class);
        var driveItemRequestBuilder = mock(DriveItemRequestBuilder.class);
        var itemsRequestBuilder = mock(ItemsRequestBuilder.class);
        var itemRequestBuilder = mock(DriveItemItemRequestBuilder.class);

        when(graphServiceClientMock.drives()).thenReturn(drivesRequestBuilder);
        when(drivesRequestBuilder.byDriveId(anyString())).thenReturn(driveItemRequestBuilder);
        when(driveItemRequestBuilder.items()).thenReturn(itemsRequestBuilder);
        when(itemsRequestBuilder.byDriveItemId(anyString())).thenReturn(itemRequestBuilder);
        doThrow(exception).when(itemRequestBuilder).delete();
    }
}
