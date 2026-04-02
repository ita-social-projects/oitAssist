package com.itasocialacademy.oitassist.filemanager.controller;

import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/file")
@RequiredArgsConstructor
@Tag(name = "File Manager V1", description = "Operations related to file management")
public class FileController {
    private final FileService fileService;

    @Operation(summary = "Soft delete file", description = "Marks the DB record SOFT_DELETED, file remains intact")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "File deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "File not found in the DB")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG','USER')")
    public ResponseEntity<Void> deleteSoft(@PathVariable Long id) {
        fileService.deleteSoft(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Hard delete file", description = "Marks the DB record HARD_DELETED, file is deleted")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "File deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "File not found in the DB")
    })
    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    public ResponseEntity<Void> deleteHard(@PathVariable Long id) {
        fileService.deleteHard(id);
        return ResponseEntity.noContent().build();
    }
}