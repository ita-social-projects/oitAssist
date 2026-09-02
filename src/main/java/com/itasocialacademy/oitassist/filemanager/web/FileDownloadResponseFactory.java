package com.itasocialacademy.oitassist.filemanager.web;

import com.itasocialacademy.oitassist.filemanager.dto.response.FileDownloadDto;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Builds the HTTP response for file downloads, enforcing safe Content-Type
 * resolution and Content-Disposition (inline vs attachment) rules.
 */
@Slf4j
@Component
public class FileDownloadResponseFactory {
    private static final String DEFAULT_DOWNLOAD_FILENAME = "download";

    private static final Set<String> SAFE_INLINE_MIME_TYPES = Set.of(
        "image/png",
        "image/jpeg",
        "image/gif",
        "image/webp",
        "application/pdf");

    public ResponseEntity<Resource> build(FileDownloadDto dto) {
        MediaType mediaType = resolveMediaType(dto.mimeType());

        var responseBuilder = ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(dto.displayFilename(), mediaType))
            .header("X-Content-Type-Options", "nosniff");

        if (dto.contentLength() != null && dto.contentLength() > 0) {
            responseBuilder.contentLength(dto.contentLength());
        }

        return responseBuilder.body(dto.resource());
    }

    /**
     * Resolves the {@link MediaType} from a MIME type string, falling back to
     * {@link MediaType#APPLICATION_OCTET_STREAM} if null, empty, or unparseable.
     *
     * @param mimeType the MIME type string (e.g., {@code "image/png"})
     * @return the resolved {@link MediaType}
     */
    private MediaType resolveMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (InvalidMediaTypeException e) {
            log.warn("Malformed MIME type '{}', falling back to APPLICATION_OCTET_STREAM", mimeType);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * Constructs a {@code Content-Disposition} header value with UTF-8 filename
     * encoding. Uses {@code inline} only for a whitelist of safe, renderable MIME
     * types; everything else is forced to {@code attachment} so the browser never
     * attempts to render potentially unsafe content in-page.
     *
     * @param filename  the original filename, or fallback to default if blank
     * @param mediaType the resolved MIME type of the file
     * @return the formatted {@code Content-Disposition} header string
     */
    private String buildContentDisposition(String filename, MediaType mediaType) {
        String resolvedFilename = (filename != null && !filename.isBlank())
            ? filename
            : DEFAULT_DOWNLOAD_FILENAME;

        var builder = isSafeForInline(mediaType)
            ? ContentDisposition.inline()
            : ContentDisposition.attachment();

        return builder
            .filename(resolvedFilename, StandardCharsets.UTF_8)
            .build()
            .toString();
    }

    /**
     * Returns {@code true} when the media type is safe to display inline
     * (whitelisted).
     */
    private boolean isSafeForInline(MediaType mediaType) {
        String typeAndSubtype = mediaType.getType() + "/" + mediaType.getSubtype();
        return SAFE_INLINE_MIME_TYPES.contains(typeAndSubtype);
    }
}
