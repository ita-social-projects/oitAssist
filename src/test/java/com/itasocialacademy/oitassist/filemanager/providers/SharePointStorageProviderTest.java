package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileListingException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.exceptions.InvalidFilePathException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileDeleteException;
import com.itasocialacademy.oitassist.filemanager.properties.GraphProperties;
import com.microsoft.graph.drives.DrivesRequestBuilder;
import com.microsoft.graph.drives.item.DriveItemRequestBuilder;
import com.microsoft.graph.drives.item.items.ItemsRequestBuilder;
import com.microsoft.graph.drives.item.items.item.DriveItemItemRequestBuilder;
import com.microsoft.graph.drives.item.items.item.children.ChildrenRequestBuilder;
import com.microsoft.graph.drives.item.items.item.content.ContentRequestBuilder;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.models.Folder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
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
    private static final String DRIVE_ID = "test-drive-id";

    private SharePointStorageProvider sharePointStorageProvider;

    @Mock
    private GraphServiceClient graphServiceClientMock;

    @Mock
    private GraphProperties graphPropertiesMock;

    @Mock
    private DrivesRequestBuilder drivesBuilder;

    @Mock
    private DriveItemRequestBuilder driveItemBuilder;

    @Mock
    private ItemsRequestBuilder itemsBuilder;

    @Mock
    private DriveItemItemRequestBuilder itemBuilder;

    @Mock
    private ContentRequestBuilder contentBuilder;

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
        stubUpload();

        String result = sharePointStorageProvider.upload(inputStream, fileName, null);
        assertThat(result).isEqualTo(fileName);
    }

    @Test
    void upload_ShouldReturnStorageKeyWithJustFileName_WhenPathIsBlank() {
        String fileName = "report.xlsx";
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());
        stubUpload();

        String result = sharePointStorageProvider.upload(inputStream, fileName, "   ");
        assertThat(result).isEqualTo(fileName);
    }

    @Test
    void upload_ShouldReturnStorageKeyWithPathAndFileName_WhenPathIsProvided() {
        String fileName = "photo.jpg";
        String path = "images/gallery";
        InputStream inputStream = new ByteArrayInputStream("image-data".getBytes());
        stubUpload();

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

        stubUploadToThrow(new RuntimeException("Network error"));

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
        stubGraphChain();
        assertThatNoException()
            .isThrownBy(() -> sharePointStorageProvider.deletePhysical("images/photo.jpg"));

        verify(itemBuilder).delete();
    }

    @Test
    void deletePhysical_ShouldReturnSilently_WhenFileNotFoundInSharePoint() {
        stubGraphChain();
        doThrow(apiException(404)).when(itemBuilder).delete();

        assertThatNoException()
            .isThrownBy(() -> sharePointStorageProvider.deletePhysical("missing/file.pdf"));
    }

    @Test
    void deletePhysical_ShouldThrowFileDeleteException_WhenApiExceptionIsNot404() {
        stubGraphChain();
        ApiException serverError = apiException(500);
        doThrow(serverError).when(itemBuilder).delete();

        assertThatThrownBy(() -> sharePointStorageProvider.deletePhysical("documents/report.docx"))
            .isInstanceOf(FileDeleteException.class)
            .hasMessageContaining("Failed to delete file from SharePoint")
            .hasCause(serverError);
    }

    @Test
    void deletePhysical_ShouldThrowFileDeleteException_WhenUnexpectedExceptionOccurs() {
        stubGraphChain();
        doThrow(new RuntimeException("Unexpected failure")).when(itemBuilder).delete();

        assertThatThrownBy(() -> sharePointStorageProvider.deletePhysical("docs/contract.pdf"))
            .isInstanceOf(FileDeleteException.class)
            .hasMessageContaining("Failed to delete file from SharePoint");
    }

    //
    // listAllPhysicalKeys() SECTION TESTS
    //

    @Test
    void listAllPhysicalKeys_ShouldReturnFileKeys_WhenRootContainsOnlyFiles() {
        stubGraphChain();
        stubChildren("root", List.of(driveFile("report.pdf"), driveFile("photo.jpg")), null);

        assertThat(sharePointStorageProvider.listAllPhysicalKeys())
            .containsExactlyInAnyOrder("report.pdf", "photo.jpg");
    }

    @Test
    void listAllPhysicalKeys_ShouldReturnEmptyList_WhenRootIsEmpty() {
        stubGraphChain();
        stubChildren("root", List.of(), null);

        assertThat(sharePointStorageProvider.listAllPhysicalKeys()).isEmpty();
    }

    @Test
    void listAllPhysicalKeys_ShouldFollowNextLink_WhenPageIsPaginated() {
        stubGraphChain();
        stubChildrenPaginated("root",
            List.of(driveFile("file1.pdf")), "https://next-page-url",
            List.of(driveFile("file2.pdf")));

        assertThat(sharePointStorageProvider.listAllPhysicalKeys())
            .containsExactlyInAnyOrder("file1.pdf", "file2.pdf");
    }

    @Test
    void listAllPhysicalKeys_ShouldReturnNestedFileKeys_WhenRootContainsFolderWithFiles() {
        stubGraphChain();
        stubChildren("root", List.of(driveFolder("images", "folder-id-1")), null);
        stubChildren("folder-id-1", List.of(driveFile("photo.jpg")), null);

        assertThat(sharePointStorageProvider.listAllPhysicalKeys())
            .containsExactlyInAnyOrder("images/photo.jpg");
    }

    @Test
    void listAllPhysicalKeys_ShouldThrowFileListingException_WhenGraphApiThrowsException() {
        stubGraphChain();
        var localItemBuilder = mock(DriveItemItemRequestBuilder.class);
        var localChildrenBuilder = mock(ChildrenRequestBuilder.class);
        when(itemsBuilder.byDriveItemId("root")).thenReturn(localItemBuilder);
        when(localItemBuilder.children()).thenReturn(localChildrenBuilder);
        when(localChildrenBuilder.get()).thenThrow(new RuntimeException("Network error"));

        assertThatThrownBy(() -> sharePointStorageProvider.listAllPhysicalKeys())
            .isInstanceOf(FileListingException.class)
            .hasMessageContaining("Failed to list files from SharePoint")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    //
    // getLastModified() SECTION TESTS
    //

    @Test
    void getLastModified_ShouldThrowInvalidFilePathException_WhenStorageKeyIsBlank() {
        assertThatThrownBy(() -> sharePointStorageProvider.getLastModified("   "))
            .isInstanceOf(InvalidFilePathException.class)
            .hasMessageContaining("blank storage key");
    }

    @Test
    void getLastModified_ShouldReturnDateTime_WhenFileExistsWithTimestamp() {
        stubGraphChain();
        OffsetDateTime expectedTime = OffsetDateTime.parse("2024-06-01T10:00:00Z");
        DriveItem item = new DriveItem();
        item.setLastModifiedDateTime(expectedTime);
        when(itemBuilder.get()).thenReturn(item);

        assertThat(sharePointStorageProvider.getLastModified("docs/file.pdf"))
            .isEqualTo(expectedTime);
    }

    @Test
    void getLastModified_ShouldReturnNull_WhenItemHasNoTimestamp() {
        stubGraphChain();
        DriveItem item = new DriveItem();
        item.setLastModifiedDateTime(null);
        when(itemBuilder.get()).thenReturn(item);

        assertThat(sharePointStorageProvider.getLastModified("docs/file.pdf")).isNull();
    }

    @Test
    void getLastModified_ShouldReturnNull_WhenItemIsNull() {
        stubGraphChain();
        when(itemBuilder.get()).thenReturn(null);

        assertThat(sharePointStorageProvider.getLastModified("docs/file.pdf")).isNull();
    }

    @Test
    void getLastModified_ShouldReturnNull_WhenFileNotFoundInSharePoint() {
        stubGraphChain();
        ApiException exception = apiException(404);
        when(itemBuilder.get()).thenThrow(exception);

        assertThat(sharePointStorageProvider.getLastModified("missing/file.pdf")).isNull();
    }

    @Test
    void getLastModified_ShouldThrowFileListingException_WhenApiExceptionIsNot404() {
        stubGraphChain();
        ApiException serverError = apiException(503);
        when(itemBuilder.get()).thenThrow(serverError);

        assertThatThrownBy(() -> sharePointStorageProvider.getLastModified("docs/file.pdf"))
            .isInstanceOf(FileListingException.class)
            .hasMessageContaining("Failed to get last modified from SharePoint")
            .hasCause(serverError);
    }

    @Test
    void getLastModified_ShouldThrowFileListingException_WhenUnexpectedExceptionOccurs() {
        stubGraphChain();
        when(itemBuilder.get()).thenThrow(new RuntimeException("Timeout"));

        assertThatThrownBy(() -> sharePointStorageProvider.getLastModified("docs/file.pdf"))
            .isInstanceOf(FileListingException.class)
            .hasMessageContaining("Failed to get last modified from SharePoint")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    //
    // HELPER METHODS
    //

    /**
     * Stubs the shared Graph API fluent chain: drives() -> byDriveId() -> items()
     * -> byDriveItemId().
     */
    private void stubGraphChain() {
        when(graphPropertiesMock.getDriveId()).thenReturn(DRIVE_ID);
        when(graphServiceClientMock.drives()).thenReturn(drivesBuilder);
        when(drivesBuilder.byDriveId(anyString())).thenReturn(driveItemBuilder);
        when(driveItemBuilder.items()).thenReturn(itemsBuilder);
        when(itemsBuilder.byDriveItemId(anyString())).thenReturn(itemBuilder);
    }

    /**
     * Stubs the full upload chain to complete successfully via content().put().
     */
    private void stubUpload() {
        stubGraphChain();
        when(itemBuilder.content()).thenReturn(contentBuilder);
        when(contentBuilder.put(any(InputStream.class))).thenReturn(null);
    }

    /**
     * Stubs the full upload chain to throw the given exception at content().put().
     */
    private void stubUploadToThrow(Exception ex) {
        stubGraphChain();
        when(itemBuilder.content()).thenReturn(contentBuilder);
        when(contentBuilder.put(any(InputStream.class))).thenThrow(ex);
    }

    /**
     * Stubs a single-page children listing for the given itemId; uses exact
     * matching to support multiple IDs per test.
     */
    private void stubChildren(String itemId, List<DriveItem> items, String nextLink) {
        var localItemBuilder = mock(DriveItemItemRequestBuilder.class);
        var localChildrenBuilder = mock(ChildrenRequestBuilder.class);

        var page = mock(DriveItemCollectionResponse.class);
        when(page.getValue()).thenReturn(items);
        when(page.getOdataNextLink()).thenReturn(nextLink);

        when(itemsBuilder.byDriveItemId(itemId)).thenReturn(localItemBuilder);
        when(localItemBuilder.children()).thenReturn(localChildrenBuilder);
        when(localChildrenBuilder.get()).thenReturn(page);
    }

    /**
     * Stubs a two-page paginated children listing: first page returns firstItems +
     * nextLink, second page returns secondItems.
     */
    private void stubChildrenPaginated(String itemId,
        List<DriveItem> firstItems, String nextLink,
        List<DriveItem> secondItems) {
        var localItemBuilder = mock(DriveItemItemRequestBuilder.class);
        var firstChildrenBuilder = mock(ChildrenRequestBuilder.class);
        var nextChildrenBuilder = mock(ChildrenRequestBuilder.class);

        var firstPage = mock(DriveItemCollectionResponse.class);
        when(firstPage.getValue()).thenReturn(firstItems);
        when(firstPage.getOdataNextLink()).thenReturn(nextLink);

        var secondPage = mock(DriveItemCollectionResponse.class);
        when(secondPage.getValue()).thenReturn(secondItems);
        when(secondPage.getOdataNextLink()).thenReturn(null);

        when(itemsBuilder.byDriveItemId(itemId)).thenReturn(localItemBuilder);
        when(localItemBuilder.children()).thenReturn(firstChildrenBuilder);
        when(firstChildrenBuilder.get()).thenReturn(firstPage);
        when(firstChildrenBuilder.withUrl(nextLink)).thenReturn(nextChildrenBuilder);
        when(nextChildrenBuilder.get()).thenReturn(secondPage);
    }

    /**
     * Creates a mock ApiException with the given HTTP status code.
     */
    private static ApiException apiException(int statusCode) {
        ApiException ex = mock(ApiException.class);
        when(ex.getResponseStatusCode()).thenReturn(statusCode);
        return ex;
    }

    /**
     * Creates a mock DriveItem representing a plain file (no folder metadata).
     */
    private DriveItem driveFile(String name) {
        DriveItem item = new DriveItem();
        item.setName(name);
        item.setFolder(null);
        return item;
    }

    /**
     * Creates a mock DriveItem representing a folder with a known Graph item ID.
     */
    private DriveItem driveFolder(String name, String id) {
        DriveItem item = new DriveItem();
        item.setName(name);
        item.setId(id);
        item.setFolder(new Folder());
        return item;
    }
}
