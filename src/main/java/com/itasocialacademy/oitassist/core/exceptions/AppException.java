package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import lombok.Getter;

/**
 * Base class for all application-specific runtime exceptions.
 * <p>
 * {@code AppException} represents a logical failure that occurred during
 * application execution and is part of the application's error contract. All
 * custom exceptions that are intended to be handled and converted into
 * structured API error responses must extend this class.
 * </p>
 *
 * <h2>Architectural role</h2>
 * <ul>
 * <li>Located in the <b>core</b> module to be accessible across all application
 * layers.</li>
 * <li>Does <b>not</b> depend on any web or transport-level concerns (e.g.
 * HTTP).</li>
 * <li>Acts as a stable boundary between business logic and error
 * representation.</li>
 * </ul>
 *
 * <h2>ErrorCode contract</h2>
 * <p>
 * Each {@code AppException} must be associated with an {@link ErrorCode} that
 * uniquely identifies the error scenario. This code is used for:
 * </p>
 * <ul>
 * <li>API error responses</li>
 * <li>Client-side error handling</li>
 * <li>Logging and monitoring</li>
 * </ul>
 *
 * <p>
 * Mapping between {@link ErrorCode} and HTTP status codes is performed outside
 * of this class (typically in the web layer via {@code @ControllerAdvice}).
 * </p>
 *
 * <h2>Usage guidelines</h2>
 * <ul>
 * <li>Extend this class for all domain or technical exceptions.</li>
 * <li>Do not throw {@code AppException} directly; always use a meaningful
 * subclass.</li>
 * <li>Do not embed HTTP status codes or transport-level logic here.</li>
 * </ul>
 *
 * @see ErrorCode
 */
@Getter
public abstract class AppException extends RuntimeException {
    /**
     * Application-specific error code that uniquely identifies the error type.
     */
    private final ErrorCode errorCode;

    /**
     * Constructs a new application exception with a human-readable message and a
     * corresponding {@link ErrorCode}.
     *
     * @param message   human-readable error description (for logs and API
     *                  responses)
     * @param errorCode application-specific error identifier
     */
    protected AppException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected AppException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}