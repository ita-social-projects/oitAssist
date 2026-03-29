package com.itasocialacademy.oitassist.filemanager.controller;

import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.FileNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Manager V1", description = "Operations related to file management")
public class FileManagerController {
    private final FileManagerService fileService;

    @Operation(summary = "Soft delete file", description = "Marks the DB record SOFT_DELETED, file remains intact")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "File deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSoft(@PathVariable Long id) throws FileNotFoundException {
        fileService.deleteSoft(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Hard delete file", description = "Marks the DB record HARD_DELETED, file is deleted")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "File deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    public ResponseEntity<Void> deleteHard(@PathVariable Long id) throws FileNotFoundException {
        fileService.deleteHard(id);
        return ResponseEntity.noContent().build();
    }
}
