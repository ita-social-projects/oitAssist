package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Represents technical failures occurring during data access operations.
 * <p>
 * Thrown when the application cannot perform a database or persistence-layer
 * operation, such as query failures, constraint violations, or connection
 * issues.
 * </p>
 *
 * <h2>Architectural role</h2>
 * <ul>
 * <li>Thrown from DAO or repository layer, or lower-level services interacting
 * with persistence.</li>
 * <li>Indicates a system-level problem, not a business or client error.</li>
 * <li>Typically mapped to HTTP 500 (Internal Server Error) or equivalent in API
 * responses.</li>
 * </ul>
 *
 * <h2>Contextual information</h2>
 * <p>
 * Details about the underlying cause can be included via the
 * {@link Throwable#getCause()} mechanism. Additional context (e.g., query
 * parameters, entity ID) should be logged via MDC or standard logging.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 * <li>Use this exception to wrap low-level persistence or infrastructure
 * errors.</li>
 * <li>Do not use it for business rule violations or validation errors.</li>
 * </ul>
 *
 * @see TechnicalException
 * @see AppException
 * @see ErrorCode
 */
public class DataAccessException extends TechnicalException {
    public DataAccessException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
