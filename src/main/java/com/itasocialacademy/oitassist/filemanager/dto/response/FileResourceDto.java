package com.itasocialacademy.oitassist.filemanager.dto.response;

import org.springframework.core.io.Resource;

/**
 * Data transfer object containing the necessary information for a controller to
 * construct a file retrieve or display response.
 *
 * @param resource        the Spring {@link Resource} providing the file's byte
 *                        stream
 * @param mimeType        the MIME media type of the file (e.g.,
 *                        {@code "application/pdf"})
 * @param displayFilename the filename to use for Content-Disposition
 * @param contentLength   the file size in bytes, or {@code null} if unknown
 */
public record FileResourceDto(
    Resource resource,
    String mimeType,
    String displayFilename,
    Long contentLength) {
}
