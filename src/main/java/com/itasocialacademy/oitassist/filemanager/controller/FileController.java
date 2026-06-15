package com.itasocialacademy.oitassist.filemanager.controller;

import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileCleanupService;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Manager V1", description = "Operations related to file management")
public class FileController {
    private final FileService fileService;
    private final FileCleanupService cleanupService;

    /**
     * Validates and uploads a batch of files, persisting their metadata and linking
     * them to the specified entity. Returns the saved file records on success.
     *
     * @param files      the files to upload
     * @param requestDto upload context metadata (entity type and optional entity
     *                   ID)
     * @return HTTP 201 with the list of persisted file records
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload files",
        description = """
            Uploads one or more files, stores their metadata, and links them to the specified related entity.
            """,
        requestBody = @RequestBody(
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                encoding = @Encoding(name = "metadata", contentType = "application/json"))))
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Files uploaded successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = FileResponseDto.class)))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid upload request",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FileResponseDto>> upload(
        @RequestPart("files") List<MultipartFile> files,
        @RequestPart("metadata") @Valid FileUploadRequestDto requestDto) {
        List<FileResponseDto> response = fileService.upload(files, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Marks the file record as SOFT_DELETED. The physical file remains in storage
     * and will be purged during the next cleanup cycle.
     *
     * @param id the ID of the file to soft-delete
     * @return HTTP 204 on success
     */
    @Operation(
        summary = "Soft delete file",
        description = "Marks the DB record SOFT_DELETED, file remains intact")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "File deleted successfully"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied"),
        @ApiResponse(
            responseCode = "404",
            description = "File not found in the DB")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG','USER')")
    public ResponseEntity<Void> deleteSoft(@PathVariable Long id) {
        fileService.deleteSoft(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently deletes the file from both the physical storage and the database
     * by marking the record as HARD_DELETED.
     *
     * @param id the ID of the file to hard-delete
     * @return HTTP 204 on success
     */
    @Operation(
        summary = "Hard delete file",
        description = "Marks the DB record HARD_DELETED, file is deleted")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "File deleted successfully"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied"),
        @ApiResponse(
            responseCode = "404",
            description = "File not found in the DB")
    })
    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    public ResponseEntity<Void> deleteHard(@PathVariable Long id) {
        fileService.deleteHard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Triggers the full file cleanup cycle immediately, without waiting for the
     * next scheduled execution. Restricted to administrators.
     *
     * @return HTTP 204 on success
     */
    @Operation(
        summary = "Trigger manual file cleanup",
        description = "Forces the file cleanup logic to run immediately for orphaned and expired files.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Cleanup triggered successfully"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied")
    })
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> triggerManualCleanup() {
        log.info("Admin triggered manual file cleanup.");
        cleanupService.runFullCleanup();
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Get files by entity",
        description = """
            Returns all files with status ATTACHED for the given entity type and ID.
            Returns an empty list if no files are found — this is not an error condition.
            """)
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Files retrieved successfully. Empty list if none found.",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = FileResponseDto.class)))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid entityType value",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    public ResponseEntity<List<FileResponseDto>> getFiles(
        @RequestParam RelatedEntityType entityType,
        @RequestParam Long entityId) {
        return ResponseEntity.ok(fileService.getFilesByEntity(entityType, entityId));
    }
}