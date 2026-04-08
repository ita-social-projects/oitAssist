package com.itasocialacademy.oitassist.filemanager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class FileControllerTest extends ControllerUnitTest<FileController> {

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileController fileController;

    @Override
    protected FileController getController() {
        return fileController;
    }

    private MockMultipartFile singleFilePart() {
        return new MockMultipartFile("files", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "image-bytes".getBytes());
    }

    private MockMultipartFile metadataPart(FileUploadRequestDto dto) throws Exception {
        return new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(dto));
    }

    // --- Upload Tests ---

    @Test
    void upload_ShouldReturnCreatedWithBody_WhenFilesUploadedSuccessfully() throws Exception {
        FileUploadRequestDto requestDto = FileUploadRequestDto.builder()
            .relatedEntityType(RelatedEntityType.NEWS)
            .relatedEntityId(10L)
            .build();
        List<FileResponseDto> serviceResponse = List.of(
            FileResponseDto.builder()
                .id(1L)
                .storageKey("uploads/photo.jpg")
                .mimeType(MediaType.IMAGE_JPEG_VALUE)
                .size(1024L)
                .build());

        when(fileService.upload(any(), any(), any())).thenReturn(serviceResponse);

        mockMvc.perform(multipart("/api/v1/files")
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

        when(fileService.upload(any(), any(), eq(null))).thenReturn(List.of());

        mockMvc.perform(multipart("/api/v1/files")
            .file(singleFilePart())
            .file(metadataPart(requestDto)))
            .andExpect(status().isCreated());

        verify(fileService).upload(any(), any(), eq(null));
    }

    @Test
    void upload_ShouldReturnBadRequest_WhenRelatedEntityTypeIsMissing() throws Exception {
        FileUploadRequestDto requestDto = FileUploadRequestDto.builder()
            .relatedEntityId(10L)
            .build();

        mockMvc.perform(multipart("/api/v1/files")
            .file(singleFilePart())
            .file(metadataPart(requestDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void upload_ShouldReturnBadRequest_WhenMetadataPartIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/v1/files")
            .file(singleFilePart()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void upload_ShouldReturnBadRequest_WhenFilesPartIsMissing() throws Exception {
        FileUploadRequestDto requestDto = FileUploadRequestDto.builder()
            .relatedEntityType(RelatedEntityType.NEWS)
            .relatedEntityId(10L)
            .build();

        mockMvc.perform(multipart("/api/v1/files")
            .file(metadataPart(requestDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void upload_ShouldReturnInternalServerError_WhenServiceThrowsFileUploadException() throws Exception {
        FileUploadRequestDto requestDto = FileUploadRequestDto.builder()
            .relatedEntityType(RelatedEntityType.NEWS)
            .relatedEntityId(10L)
            .build();

        when(fileService.upload(any(), any(), any()))
            .thenThrow(new FileUploadException("photo.jpg", new RuntimeException("I/O error")));

        mockMvc.perform(multipart("/api/v1/files")
            .file(singleFilePart())
            .file(metadataPart(requestDto)))
            .andExpect(status().isInternalServerError());
    }
}