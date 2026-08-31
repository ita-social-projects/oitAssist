package com.itasocialacademy.oitassist.filemanager.dto.response;

import org.springframework.core.io.Resource;

/**
 * Data transfer object containing the necessary information for a controller to
 * construct a file download or display response.
 *
 * @param resource         the Spring {@link Resource} providing the file's byte
 *                         stream
 * @param mimeType         the MIME media type of the file (e.g.,
 *                         {@code "application/pdf"})
 * @param originalFilename the original filename provided upon upload, used for
 *                         Content-Disposition
 * @param contentLength    the file size in bytes, or {@code null} if unknown
 */
public record FileDownloadDto(
    Resource resource,
    String mimeType,
    String originalFilename,
    Long contentLength) {
}
