package com.itasocialacademy.oitassist.filemanager.controller;

import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileManagerService;
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

@ExtendWith(MockitoExtension.class)
class FileManagerControllerTest {

    @Mock
    private FileManagerService fileService;

    @InjectMocks
    private FileManagerController fileController;

    private static final Long FILE_ID = 0L;

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
}