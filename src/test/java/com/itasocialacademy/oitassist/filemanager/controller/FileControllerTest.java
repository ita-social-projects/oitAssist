package com.itasocialacademy.oitassist.filemanager.controller;

import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

//add MockMvc to perform requests and verify @PreAuthorize with @WithMockUser

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileController fileController;

    private static final Long FILE_ID = 0L;

    // --- Soft Delete Tests ---

    @Test
    void deleteSoft_ShouldReturnNoContent_WhenSuccessful() throws FileNotFoundException {
        doNothing().when(fileService).deleteSoft(FILE_ID);
        ResponseEntity<Void> response = fileController.deleteSoft(FILE_ID);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(fileService, times(1)).deleteSoft(FILE_ID);
    }

    @Test
    void deleteSoft_ShouldThrowException_WhenServiceThrows() throws FileNotFoundException {
        doThrow(new FileNotFoundException("File not found"))
            .when(fileService).deleteSoft(FILE_ID);

        assertThrows(FileNotFoundException.class, () -> fileController.deleteSoft(FILE_ID));

        verify(fileService, times(1)).deleteSoft(FILE_ID);
    }

    // --- Hard Delete Tests ---

    @Test
    void deleteHard_ShouldReturnNoContent_WhenSuccessful() throws FileNotFoundException {
        doNothing().when(fileService).deleteHard(FILE_ID);

        ResponseEntity<Void> response = fileController.deleteHard(FILE_ID);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(fileService, times(1)).deleteHard(FILE_ID);
    }

    @Test
    void deleteHard_ShouldThrowException_WhenServiceThrows() throws FileNotFoundException {
        doThrow(new FileNotFoundException("File not found"))
            .when(fileService).deleteHard(FILE_ID);

        assertThrows(FileNotFoundException.class, () -> fileController.deleteHard(FILE_ID));
        verify(fileService, times(1)).deleteHard(FILE_ID);
    }
}