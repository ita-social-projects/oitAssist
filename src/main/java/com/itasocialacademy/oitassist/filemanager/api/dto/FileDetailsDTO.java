package com.itasocialacademy.oitassist.filemanager.api.dto;

/**
 * Cross-module DTO representing file metadata and download URL. Used by other
 * modules to receive file information from the file manager.
 */
public record FileDetailsDTO(
    Long id,
    String originalFilename,
    String mimeType,
    Long size,
    String fileRole,
    String url) {
}
