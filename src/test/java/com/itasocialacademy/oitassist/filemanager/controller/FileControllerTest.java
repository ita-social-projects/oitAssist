package com.itasocialacademy.oitassist.filemanager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileAssetNotFoundException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileCleanupService;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class FileControllerTest extends ControllerUnitTest<FileController> {

    private static final String FILES_URL = "/api/v1/files";
    private static final String FILE_BY_ID_URL = "/api/v1/files/{id}";
    private static final String FILE_BY_ID_HARD_URL = "/api/v1/files/{id}/hard";
    private static final String FILES_CLEANUP_URL = "/api/v1/files/cleanup";
    private static final Long EXISTING_FILE_ID = 1L;
    private static final Long NON_EXISTING_FILE_ID = 999L;
    private static final Long RELATED_ENTITY_ID = 10L;
    private static final String ACCESS_DENIED_MESSAGE = "Access denied";
    private static final String FILE_NOT_FOUND_MESSAGE = "File not found";

    @Mock
    private FileService fileService;

    @Mock
    private FileCleanupService cleanupService;

    @InjectMocks
    private FileController fileController;

    @Override
    protected FileController getController() {
        return fileController;
    }

    private MockMultipartFile singleFilePart() {
        return new MockMultipartFile("files", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "image-bytes".getBytes());
    }

    private MockMultipartFile metadataPart(FileUploadRequestDto dto) {
        return new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(dto));
    }

    // --- Upload Tests ---

    @Test
    void upload_ShouldReturnCreatedWithBody_WhenFilesUploadedSuccessfully() throws Exception {
        FileUploadRequestDto requestDto = FileUploadRequestDto.builder()
            .relatedEntityType(RelatedEntityType.NEWS)
            .relatedEntityId(RELATED_ENTITY_ID)
            .build();
        List<FileResponseDto> serviceResponse = List.of(
            FileResponseDto.builder()
                .id(1L)
                .storageKey("uploads/photo.jpg")
                .mimeType(MediaType.IMAGE_JPEG_VALUE)
                .size(1024L)
                .build());

        when(fileService.upload(any(), any())).thenReturn(serviceResponse);

        mockMvc.perform(multipart(FILES_URL)
            .file(singleFilePart())
            .file(metadataPart(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].storageKey").value("uploads/photo.jpg"))
            .andExpect(jsonPath("$[0].mimeType").value(MediaType.IMAGE_JPEG_VALUE))
            .andExpect(jsonPath("$[0].size").value(1024));
    }

    @Test
    void upload_ShouldDelegateToServiceWithNullUserId_WhenFilesAndMetadataAreValid() throws Exception {
        // standaloneSetup does not process @AuthenticationPrincipal,
        // so currentUserId is always null in unit test scope.
        FileUploadRequestDto requestDto = FileUploadRequestDto.builder()
            .relatedEntityType(RelatedEntityType.TASK)
            .relatedEntityId(5L)
            .build();

        when(fileService.upload(any(), any())).thenReturn(List.of());

        mockMvc.perform(multipart(FILES_URL)
            .file(singleFilePart())
            .file(metadataPart(requestDto)))
            .andExpect(status().isCreated());

        verify(fileService).upload(any(), any());
    }

    @Test
    void upload_ShouldReturnBadRequest_WhenRelatedEntityTypeIsMissing() throws Exception {
        FileUploadRequestDto requestDto = FileUploadRequestDto.builder()
            .relatedEntityId(RELATED_ENTITY_ID)
            .build();

        mockMvc.perform(multipart(FILES_URL)
            .file(singleFilePart())
            .file(metadataPart(requestDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void upload_ShouldReturnBadRequest_WhenMetadataPartIsMissing() throws Exception {
        mockMvc.perform(multipart(FILES_URL)
            .file(singleFilePart()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void upload_ShouldReturnBadRequest_WhenFilesPartIsMissing() throws Exception {
        FileUploadRequestDto requestDto = FileUploadRequestDto.builder()
            .relatedEntityType(RelatedEntityType.NEWS)
            .relatedEntityId(RELATED_ENTITY_ID)
            .build();

        mockMvc.perform(multipart(FILES_URL)
            .file(metadataPart(requestDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void upload_ShouldReturnInternalServerError_WhenServiceThrowsFileUploadException() throws Exception {
        FileUploadRequestDto requestDto = FileUploadRequestDto.builder()
            .relatedEntityType(RelatedEntityType.NEWS)
            .relatedEntityId(RELATED_ENTITY_ID)
            .build();

        when(fileService.upload(any(), any()))
            .thenThrow(new FileUploadException("photo.jpg", new RuntimeException("I/O error")));

        mockMvc.perform(multipart(FILES_URL)
            .file(singleFilePart())
            .file(metadataPart(requestDto)))
            .andExpect(status().isInternalServerError());
    }

    // --- Delete Tests ---

    @Test
    void deleteSoft_ShouldReturnNoContent_WhenFileExists() throws Exception {
        Long fileId = EXISTING_FILE_ID;

        mockMvc.perform(delete(FILE_BY_ID_URL, fileId))
            .andExpect(status().isNoContent());

        verify(fileService).deleteSoft(fileId);
    }

    @Test
    void deleteHard_ShouldReturnNoContent_WhenFileExists() throws Exception {
        Long fileId = EXISTING_FILE_ID;

        mockMvc.perform(delete(FILE_BY_ID_HARD_URL, fileId))
            .andExpect(status().isNoContent());

        verify(fileService).deleteHard(fileId);
    }

    @Test
    void deleteSoft_ShouldReturnNotFound_WhenFileDoesNotExist() throws Exception {
        Long fileId = NON_EXISTING_FILE_ID;
        doThrow(new FileAssetNotFoundException(FILE_NOT_FOUND_MESSAGE)).when(fileService).deleteSoft(fileId);

        mockMvc.perform(delete(FILE_BY_ID_URL, fileId))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteHard_ShouldReturnNotFound_WhenFileDoesNotExist() throws Exception {
        Long fileId = NON_EXISTING_FILE_ID;
        doThrow(new FileAssetNotFoundException(FILE_NOT_FOUND_MESSAGE)).when(fileService).deleteHard(fileId);

        mockMvc.perform(delete(FILE_BY_ID_HARD_URL, fileId))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteSoft_ShouldReturnForbidden_WhenUserHasNoPermission() throws Exception {
        Long fileId = EXISTING_FILE_ID;
        doThrow(new AuthorizationException(ACCESS_DENIED_MESSAGE, ErrorCode.ACCESS_DENIED))
            .when(fileService).deleteSoft(fileId);

        mockMvc.perform(delete(FILE_BY_ID_URL, fileId))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteHard_ShouldReturnForbidden_WhenUserHasNoPermission() throws Exception {
        Long fileId = EXISTING_FILE_ID;
        doThrow(new AuthorizationException(ACCESS_DENIED_MESSAGE, ErrorCode.ACCESS_DENIED))
            .when(fileService).deleteHard(fileId);

        mockMvc.perform(delete(FILE_BY_ID_HARD_URL, fileId))
            .andExpect(status().isForbidden());
    }

    // --- Cleanup Tests ---

    @Test
    void triggerManualCleanup_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete(FILES_CLEANUP_URL))
            .andExpect(status().isNoContent());

        verify(cleanupService).runFullCleanup();
    }

    @Test
    void triggerManualCleanup_ShouldReturnForbidden_WhenUserIsNotAdmin() throws Exception {
        doThrow(new AuthorizationException(ACCESS_DENIED_MESSAGE, ErrorCode.ACCESS_DENIED))
            .when(cleanupService).runFullCleanup();

        mockMvc.perform(delete(FILES_CLEANUP_URL))
            .andExpect(status().isForbidden());
    }
}
