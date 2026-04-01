package com.itasocialacademy.oitassist.filemanager.controller;

import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Manager V1", description = "Operations related to file management")
public class FileController {
    private final FileService fileService;

    @Operation(
        summary = "Upload files",
        description = """
            Uploads one or more files, stores their metadata, and links them to the specified related entity.
            """)
    @ApiResponse(
        responseCode = "201",
        description = "Files uploaded successfully",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = FileResponseDto.class))))
    @ApiResponse(
        responseCode = "400",
        description = "Invalid upload request")
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileResponseDto>> upload(
        @RequestPart("files") List<MultipartFile> files,
        @RequestPart("metadata") FileUploadRequestDto requestDto,
        @AuthenticationPrincipal(expression = "id") Long currentUserId) {
        List<FileResponseDto> response = fileService.upload(files, requestDto, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
