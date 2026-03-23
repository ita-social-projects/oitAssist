package com.itasocialacademy.oitassist.sharepoint.controller;

import com.itasocialacademy.oitassist.sharepoint.service.SharePointService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Files v1", description = "Operations related to Sharepoint")
@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    private final SharePointService sharePointService;

    public FileController(SharePointService sharePointService) {
        this.sharePointService = sharePointService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        sharePointService.uploadFile(file);
        return ResponseEntity.ok("File uploaded successfully");
    }
}
