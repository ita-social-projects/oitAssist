package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.properties.GraphProperties;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    // HELPER METHODS
    //

    /**
     * Mocks the Graph API chain to return successfully without exceptions. Sets up
     * the entire fluent chain: drives() -> byDriveId() -> items() ->
     * byDriveItemId() -> content() -> put()
     */
    private void mockGraphClientChain() {
        when(graphPropertiesMock.getDriveId()).thenReturn("test-drive-id");
        var drivesRequestBuilder = mock(com.microsoft.graph.drives.DrivesRequestBuilder.class);
        var driveItemRequestBuilder = mock(com.microsoft.graph.drives.item.DriveItemRequestBuilder.class);
        var itemsRequestBuilder = mock(com.microsoft.graph.drives.item.items.ItemsRequestBuilder.class);
        var itemRequestBuilder = mock(com.microsoft.graph.drives.item.items.item.DriveItemItemRequestBuilder.class);
        var contentRequestBuilder =
            mock(com.microsoft.graph.drives.item.items.item.content.ContentRequestBuilder.class);

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
        var drivesRequestBuilder = mock(com.microsoft.graph.drives.DrivesRequestBuilder.class);
        var driveItemRequestBuilder = mock(com.microsoft.graph.drives.item.DriveItemRequestBuilder.class);
        var itemsRequestBuilder = mock(com.microsoft.graph.drives.item.items.ItemsRequestBuilder.class);
        var itemRequestBuilder = mock(com.microsoft.graph.drives.item.items.item.DriveItemItemRequestBuilder.class);
        var contentRequestBuilder =
            mock(com.microsoft.graph.drives.item.items.item.content.ContentRequestBuilder.class);

        when(graphServiceClientMock.drives()).thenReturn(drivesRequestBuilder);
        when(drivesRequestBuilder.byDriveId(anyString())).thenReturn(driveItemRequestBuilder);
        when(driveItemRequestBuilder.items()).thenReturn(itemsRequestBuilder);
        when(itemsRequestBuilder.byDriveItemId(anyString())).thenReturn(itemRequestBuilder);
        when(itemRequestBuilder.content()).thenReturn(contentRequestBuilder);
        when(contentRequestBuilder.put(any(InputStream.class))).thenThrow(exception);
    }
}
