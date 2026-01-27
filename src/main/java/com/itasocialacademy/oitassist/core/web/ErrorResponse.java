package com.itasocialacademy.oitassist.core.web;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String message;
    private String code;
    private String path;
    private String traceId;
    private Map<String, Object> details;
}