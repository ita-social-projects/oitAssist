package com.itasocialacademy.oitassist.core.web;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Maps {@link ErrorCode} values to their corresponding {@link HttpStatus} codes.
 * <p>
 * This class serves as a bridge between the business-level error codes and the HTTP layer.
 * It allows service and business layers to remain agnostic of HTTP while still providing
 * consistent responses in the web layer.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * HttpStatus status = appExceptionHttpStatusMapper.map(errorCode);
 * return ResponseEntity.status(status).body(errorResponse);
 * }</pre>
 *
 * <h2>Architectural role</h2>
 * <ul>
 *   <li>Used by {@code ControllerAdvice} or web-layer components to map business exceptions to HTTP responses.</li>
 *   <li>Keeps service and business layers decoupled from web concerns.</li>
 * </ul>
 *
 * @see ErrorCode
 * @see HttpStatus
 */
@Component
public class AppExceptionHttpStatusMapper {
    /**
     * Maps the given {@link ErrorCode} to a corresponding {@link HttpStatus}.
     *
     * @param errorCode the error code to map; must not be null
     * @return the corresponding {@link HttpStatus} for the error code's category
     */
    public HttpStatus map(ErrorCode errorCode) {
        return switch (errorCode.getCategory()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case AUTHORIZATION -> HttpStatus.FORBIDDEN;
            case AUTHENTICATION -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case TECHNICAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
