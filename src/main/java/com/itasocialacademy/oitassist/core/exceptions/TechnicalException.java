package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Base class for all technical (infrastructure-level) failures.
 * <p>
 * {@code TechnicalException} represents unexpected errors caused by
 * infrastructure, system, or integration problems rather than business rule
 * violations.
 * </p>
 *
 * <h2>Examples</h2>
 * <ul>
 * <li>Database connectivity failures</li>
 * <li>External service timeouts</li>
 * <li>Serialization or deserialization errors</li>
 * <li>Unexpected runtime failures</li>
 * </ul>
 *
 * <h2>Architectural role</h2>
 * <ul>
 * <li>Thrown from infrastructure or service layers.</li>
 * <li>Indicates <b>non-recoverable</b> errors from a business perspective.</li>
 * <li>Signals that the request could not be processed due to a system
 * failure.</li>
 * </ul>
 *
 * <h2>HTTP mapping</h2>
 * <p>
 * Technical exceptions are typically mapped to {@code 5xx} HTTP status codes
 * (most commonly {@code 500 Internal Server Error}).
 * </p>
 *
 * <p>
 * The mapping to HTTP status codes is handled in the web layer and must not be
 * defined at this level.
 * </p>
 *
 * <h2>Usage guidelines</h2>
 * <ul>
 * <li>Extend this class for infrastructure or system-related failures.</li>
 * <li>Do <b>not</b> use this exception for business rule violations.</li>
 * <li>Always log technical exceptions with full stack trace.</li>
 * </ul>
 *
 * @see AppException
 * @see BusinessException
 * @see ErrorCode
 */
public class TechnicalException extends AppException {
    /**
     * Constructs a new technical exception with the given message and error code.
     *
     * @param message   human-readable description of the technical failure
     * @param errorCode application-specific error identifier
     */
    protected TechnicalException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
