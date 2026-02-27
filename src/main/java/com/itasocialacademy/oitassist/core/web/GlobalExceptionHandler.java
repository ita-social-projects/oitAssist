package com.itasocialacademy.oitassist.core.web;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.*;
import com.itasocialacademy.oitassist.core.exceptions.SecurityException;
import com.itasocialacademy.oitassist.core.exceptions.TechnicalException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for the entire application.
 * <p>
 * This {@link ControllerAdvice} intercepts exceptions thrown from controllers
 * or service layers and converts them into standardized {@link ErrorResponse}
 * objects for API responses.
 * </p>
 *
 * <h2>Handled Exceptions</h2>
 * <ul>
 * <li>{@link BusinessException} – domain/business errors, mapped using
 * {@link AppExceptionHttpStatusMapper}.</li>
 * <li>{@link SecurityException} – authentication/authorization failures, mapped
 * to 401/403.</li>
 * <li>{@link TechnicalException} – system/technical failures, mapped to
 * 500.</li>
 * <li>{@link MethodArgumentNotValidException} – validation errors triggered by
 * {@code @Valid} annotations.</li>
 * <li>{@link Exception} – any unhandled exceptions, mapped to 500 Internal
 * Server Error.</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 * <li>Logs exceptions with context information including {@code traceId}, user
 * ID, and IP address where applicable.</li>
 * <li>Builds {@link ErrorResponse} with timestamp, HTTP status, error code,
 * message, path, traceId, and optional details.</li>
 * <li>Uses {@link AppExceptionHttpStatusMapper} to map business error codes to
 * HTTP status codes.</li>
 * </ul>
 *
 * <h2>Trace ID</h2>
 * <p>
 * Supports tracing via:
 * <ul>
 * <li>X-Request-ID header from incoming HTTP requests</li>
 * <li>MDC ("traceId") if available</li>
 * </ul>
 * This traceId is included in logs and error responses for correlation.
 * </p>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>Service layer should throw {@link BusinessException},
 * {@link SecurityException}, or {@link TechnicalException}, not HTTP-specific
 * exceptions.</li>
 * <li>Controller layer does not need to catch exceptions manually; they are
 * automatically handled here.</li>
 * </ul>
 *
 * @see ErrorResponse
 * @see AppExceptionHttpStatusMapper
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
@NamedInterface("GlobalExceptionHandler")
public class GlobalExceptionHandler {
    private final AppExceptionHttpStatusMapper statusMapper;
    private static final String TRACE_ID_MDC = "traceId";

    private String traceIdFromRequestOrMdc(HttpServletRequest request) {
        String fromHeader = request.getHeader("X-Request-ID");
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader;
        }
        String fromMdc = MDC.get(TRACE_ID_MDC);
        return (fromMdc != null && !fromMdc.isBlank()) ? fromMdc : null;
    }

    private ErrorResponse buildResponse(HttpServletRequest request, ErrorCode errorCode, String message, Integer status,
        Map<String, Object> details) {
        String traceId = traceIdFromRequestOrMdc(request);
        return ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(status)
            .message(message)
            .code(errorCode.name())
            .traceId(traceId)
            .path(request.getRequestURI())
            .details(details)
            .build();
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn(
            "BusinessException [{}] type={} message={} traceId={}",
            ex.getErrorCode(),
            ex.getClass().getSimpleName(),
            ex.getMessage(),
            MDC.get("traceId"));
        HttpStatus status = statusMapper.map(ex.getErrorCode());
        return ResponseEntity.status(status)
            .body(buildResponse(request, ex.getErrorCode(), ex.getMessage(), status.value(), ex.getDetails()));
    }

    @ExceptionHandler(value = {SecurityException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex, HttpServletRequest request) {
        log.warn(
            "SecurityException [{}] userId={} ip={} traceId={}",
            ex.getErrorCode(),
            MDC.get("userId"),
            MDC.get("ip"),
            MDC.get("traceId"));
        HttpStatus status = statusMapper.map(ex.getErrorCode());
        return ResponseEntity.status(status)
            .body(buildResponse(request, ex.getErrorCode(), ex.getMessage(), status.value(), null));
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ErrorResponse> handleAppException(TechnicalException ex, HttpServletRequest request) {
        log.error(
            "TechnicalException [{}] type={} traceId={} path={}",
            ex.getErrorCode(),
            ex.getClass().getSimpleName(),
            MDC.get("traceId"),
            request.getRequestURI(),
            ex);

        HttpStatus status = statusMapper.map(ex.getErrorCode());
        return ResponseEntity.status(status)
            .body(buildResponse(request, ex.getErrorCode(), ex.getMessage(), status.value(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
        HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> Objects.requireNonNullElse(
                    fe.getDefaultMessage(),
                    "Invalid value"),
                (a, b) -> a + "; " + b));

        log.warn("Validation failed: traceId={}, errors={}", MDC.get("traceId"), fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(buildResponse(request, ErrorCode.COMMON_VALIDATION_FAILED, "Validation failed",
                HttpStatus.BAD_REQUEST.value(), Map.of("errors", fieldErrors)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
        HttpServletRequest request) {
        log.warn("Validation failed: traceId={}", MDC.get("traceId"));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(buildResponse(request, ErrorCode.COMMON_VALIDATION_FAILED, "Request body is missing or malformed",
                HttpStatus.BAD_REQUEST.value(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAnyException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: traceId={}", MDC.get("traceId"), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildResponse(request, ErrorCode.COMMON_INTERNAL_ERROR, "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), null));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        Exception ex,
        HttpServletRequest request) {
        log.warn("Access denied: traceId={}", MDC.get("traceId"));

        HttpStatus status = HttpStatus.FORBIDDEN;

        return ResponseEntity.status(status)
            .body(buildResponse(
                request,
                ErrorCode.ACCESS_DENIED,
                "Access denied",
                status.value(),
                null));
    }
}