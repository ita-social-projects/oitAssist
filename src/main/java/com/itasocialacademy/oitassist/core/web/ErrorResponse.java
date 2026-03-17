package com.itasocialacademy.oitassist.core.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.util.Map;

/**
 * Standard structure for API error responses.
 * <p>
 * This class is used to provide a consistent error payload to clients whenever
 * an exception occurs in the application.
 * </p>
 *
 * <h2>Fields</h2>
 * <ul>
 * <li>{@code timestamp} – the time when the error occurred (UTC).</li>
 * <li>{@code status} – the HTTP status code corresponding to the error.</li>
 * <li>{@code message} – a human-readable description of the error.</li>
 * <li>{@code code} – a machine-readable error code from
 * {@link com.itasocialacademy.oitassist.core.enums.ErrorCode}.</li>
 * <li>{@code path} – the request URI that caused the error.</li>
 * <li>{@code traceId} – a unique identifier for tracing the request through
 * logs (optional).</li>
 * <li>{@code details} – a map containing additional error details, e.g.,
 * field-specific validation errors (optional).</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * ErrorResponse response = ErrorResponse.builder()
 *     .timestamp(Instant.now())
 *     .status(HttpStatus.BAD_REQUEST.value())
 *     .message("Validation failed")
 *     .code(ErrorCode.COMMON_VALIDATION_FAILED.name())
 *     .path(request.getRequestURI())
 *     .traceId(MDC.get("traceId"))
 *     .details(Map.of("fieldErrors", fieldErrors))
 *     .build();
 * }</pre>
 *
 * <h2>Serialization</h2>
 * <p>
 * Uses Jackson annotations to include only non-null fields in the JSON
 * response.
 * </p>
 */
@Getter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response for API exceptions")
public class ErrorResponse {
    @Schema(
        description = "Timestamp of the error",
        examples = {"2026-03-17T16:00:00Z", "2026-03-18T10:30:00Z"})
    private Instant timestamp;

    @Schema(
        description = "HTTP status code",
        examples = {"403", "404"})
    private int status;

    @Schema(
        description = "Human-readable error message",
        examples = {"Access denied", "Competition not found"})
    private String message;

    @Schema(
        description = "Custom error code",
        examples = {"ACCESS_DENIED", "COMPETITION_NOT_FOUND"})
    private String code;

    @Schema(
        description = "Request path where the error occurred",
        examples = {"/api/v1/competitions", "/api/v1/competitions/123"})
    private String path;

    @Schema(
        description = "Trace ID for correlating logs",
        examples = {"d3f4c8a0-1f23-4b7b-a2e7-1234567890ab", "f9c7b0e1-9b1c-4f2a-8d7f-abcdef123456"})
    private String traceId;

    @ArraySchema(
        arraySchema = @Schema(description = "Additional details for the error"),
        schema = @Schema(
            description = "Key-value pairs with extra info",
            example = "{\"field\":\"level\",\"error\":\"must not be null\"}"))
    private Map<String, Object> details;
}