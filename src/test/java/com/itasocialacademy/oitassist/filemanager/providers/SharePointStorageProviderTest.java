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

    //
    // listAllPhysicalKeys() SECTION TESTS
    //

    @Test
    void listAllPhysicalKeys_ShouldReturnFileKeys_WhenRootContainsOnlyFiles() {
        DriveItem file1 = driveFile("report.pdf");
        DriveItem file2 = driveFile("photo.jpg");

        var itemsBuilder = mockSharedChildrenSetup();
        mockChildrenForItem(itemsBuilder, "root", List.of(file1, file2), null);

        List<String> result = sharePointStorageProvider.listAllPhysicalKeys();

        assertThat(result).containsExactlyInAnyOrder("report.pdf", "photo.jpg");
    }

    @Test
    void listAllPhysicalKeys_ShouldReturnEmptyList_WhenRootIsEmpty() {
        var itemsBuilder = mockSharedChildrenSetup();
        mockChildrenForItem(itemsBuilder, "root", List.of(), null);

        List<String> result = sharePointStorageProvider.listAllPhysicalKeys();

        assertThat(result).isEmpty();
    }

    @Test
    void listAllPhysicalKeys_ShouldFollowNextLink_WhenPageIsPaginated() {
        DriveItem file1 = driveFile("file1.pdf");
        DriveItem file2 = driveFile("file2.pdf");

        var itemsBuilder = mockSharedChildrenSetup();
        mockChildrenForItemWithPagination(itemsBuilder, "root",
            List.of(file1), "https://next-page-url", List.of(file2));

        List<String> result = sharePointStorageProvider.listAllPhysicalKeys();

        assertThat(result).containsExactlyInAnyOrder("file1.pdf", "file2.pdf");
    }

    @Test
    void listAllPhysicalKeys_ShouldReturnNestedFileKeys_WhenRootContainsFolderWithFiles() {
        DriveItem folder = driveFolder("images", "folder-id-1");
        DriveItem nestedFile = driveFile("photo.jpg");

        var itemsBuilder = mockSharedChildrenSetup();
        mockChildrenForItem(itemsBuilder, "root", List.of(folder), null);
        mockChildrenForItem(itemsBuilder, "folder-id-1", List.of(nestedFile), null);

        List<String> result = sharePointStorageProvider.listAllPhysicalKeys();

        assertThat(result).containsExactly("images/photo.jpg");
    }

    @Test
    void getLastModified_ShouldThrowFileListingException_WhenUnexpectedExceptionOccurs() {
        mockGetItemChainToThrow(new RuntimeException("Timeout"));

        assertThatThrownBy(() -> sharePointStorageProvider.getLastModified("docs/file.pdf"))
            .isInstanceOf(FileListingException.class)
            .hasMessageContaining("Failed to get last modified from SharePoint")
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
        OffsetDateTime expectedTime = OffsetDateTime.parse("2024-06-01T10:00:00Z");

        DriveItem item = new DriveItem();
        item.setLastModifiedDateTime(expectedTime);

        mockGetItemChain(item);

        OffsetDateTime result = sharePointStorageProvider.getLastModified("docs/file.pdf");

        assertThat(result).isEqualTo(expectedTime);
    }

    @Test
    void getLastModified_ShouldReturnNull_WhenItemHasNoTimestamp() {
        DriveItem item = new DriveItem();
        item.setLastModifiedDateTime(null);

        mockGetItemChain(item);

        assertThat(sharePointStorageProvider.getLastModified("docs/file.pdf")).isNull();
    }

    @Test
    void getLastModified_ShouldReturnNull_WhenItemIsNull() {
        mockGetItemChain(null);

        assertThat(sharePointStorageProvider.getLastModified("docs/file.pdf")).isNull();
    }

    @Test
    void getLastModified_ShouldReturnNull_WhenFileNotFoundInSharePoint() {
        ApiException notFound = mock(ApiException.class);
        when(notFound.getResponseStatusCode()).thenReturn(404);

        mockGetItemChainToThrow(notFound);

        assertThat(sharePointStorageProvider.getLastModified("missing/file.pdf")).isNull();
    }

    @Test
    void getLastModified_ShouldThrowFileListingException_WhenApiExceptionIsNot404() {
        ApiException serverError = mock(ApiException.class);
        when(serverError.getResponseStatusCode()).thenReturn(503);

        mockGetItemChainToThrow(serverError);

        assertThatThrownBy(() -> sharePointStorageProvider.getLastModified("docs/file.pdf"))
            .isInstanceOf(FileListingException.class)
            .hasMessageContaining("Failed to get last modified from SharePoint")
            .hasCause(serverError);
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

    /**
     * Sets up the shared trunk of the children chain (drives -> byDriveId -> items)
     * and returns the shared ItemsRequestBuilder so multiple item IDs can be
     * stubbed on the same instance without Mockito strict-stubbing conflicts.
     */
    private ItemsRequestBuilder mockSharedChildrenSetup() {
        when(graphPropertiesMock.getDriveId()).thenReturn("test-drive-id");

        var drivesBuilder = mock(DrivesRequestBuilder.class);
        var driveBuilder = mock(DriveItemRequestBuilder.class);
        var itemsBuilder = mock(ItemsRequestBuilder.class);

        when(graphServiceClientMock.drives()).thenReturn(drivesBuilder);
        when(drivesBuilder.byDriveId(anyString())).thenReturn(driveBuilder);
        when(driveBuilder.items()).thenReturn(itemsBuilder);

        return itemsBuilder;
    }

    /**
     * Stubs a single children page for a specific itemId on the shared
     * ItemsRequestBuilder. Optionally configures a nextLink for pagination.
     */
    private void mockChildrenForItem(ItemsRequestBuilder itemsBuilder, String itemId, List<DriveItem> items,
        String nextLink) {
        var itemBuilder = mock(DriveItemItemRequestBuilder.class);
        var childrenBuilder = mock(ChildrenRequestBuilder.class);

        var page = mock(DriveItemCollectionResponse.class);
        when(page.getValue()).thenReturn(items);
        when(page.getOdataNextLink()).thenReturn(nextLink);

        when(itemsBuilder.byDriveItemId(itemId)).thenReturn(itemBuilder);
        when(itemBuilder.children()).thenReturn(childrenBuilder);
        when(childrenBuilder.get()).thenReturn(page);
    }

    /**
     * Stubs a paginated children response on the shared ItemsRequestBuilder. First
     * page returns firstItems + nextLink; second page returns secondItems with no
     * further link.
     */
    private void mockChildrenForItemWithPagination(ItemsRequestBuilder itemsBuilder, String itemId,
        List<DriveItem> firstItems, String nextLink, List<DriveItem> secondItems) {
        var itemBuilder = mock(DriveItemItemRequestBuilder.class);
        var childrenBuilder = mock(ChildrenRequestBuilder.class);
        var nextBuilder = mock(ChildrenRequestBuilder.class);

        var firstPage = mock(DriveItemCollectionResponse.class);
        when(firstPage.getValue()).thenReturn(firstItems);
        when(firstPage.getOdataNextLink()).thenReturn(nextLink);

        var secondPage = mock(DriveItemCollectionResponse.class);
        when(secondPage.getValue()).thenReturn(secondItems);
        when(secondPage.getOdataNextLink()).thenReturn(null);

        when(itemsBuilder.byDriveItemId(itemId)).thenReturn(itemBuilder);
        when(itemBuilder.children()).thenReturn(childrenBuilder);
        when(childrenBuilder.get()).thenReturn(firstPage);
        when(childrenBuilder.withUrl(nextLink)).thenReturn(nextBuilder);
        when(nextBuilder.get()).thenReturn(secondPage);
    }

    /**
     * Stubs the item .get() chain to return the given DriveItem.
     */
    private void mockGetItemChain(DriveItem item) {
        when(graphPropertiesMock.getDriveId()).thenReturn("test-drive-id");

        var itemRequestBuilder = mockSharedGraphChain();
        when(itemRequestBuilder.get()).thenReturn(item);
    }

    /**
     * Stubs the item .get() chain to throw the given exception.
     */
    private void mockGetItemChainToThrow(Exception exception) {
        when(graphPropertiesMock.getDriveId()).thenReturn("test-drive-id");

        var itemRequestBuilder = mockSharedGraphChain();
        when(itemRequestBuilder.get()).thenThrow(exception);
    }

    /**
     * Sets up the shared graph chain for getLastModified operations: drives() ->
     * byDriveId() -> items() -> byDriveItemId() and returns the
     * DriveItemItemRequestBuilder for further stubbing.
     */
    private DriveItemItemRequestBuilder mockSharedGraphChain() {
        var drivesBuilder = mock(DrivesRequestBuilder.class);
        var driveBuilder = mock(DriveItemRequestBuilder.class);
        var itemsBuilder = mock(ItemsRequestBuilder.class);
        var itemBuilder = mock(DriveItemItemRequestBuilder.class);

        when(graphServiceClientMock.drives()).thenReturn(drivesBuilder);
        when(drivesBuilder.byDriveId(anyString())).thenReturn(driveBuilder);
        when(driveBuilder.items()).thenReturn(itemsBuilder);
        when(itemsBuilder.byDriveItemId(anyString())).thenReturn(itemBuilder);

        return itemBuilder;
    }

    /** Creates a mock DriveItem representing a plain file (no folder metadata). */
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
