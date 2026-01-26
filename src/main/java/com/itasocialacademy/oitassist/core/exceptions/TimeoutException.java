package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Represents a technical failure caused by an operation timing out.
 * <p>
 * Thrown when an operation, such as a call to an external service,
 * database query, or other system process, exceeds its allowed execution time.
 * </p>
 *
 * <h2>Architectural role</h2>
 * <ul>
 *   <li>Thrown from service, adapter, or technical layers that interact with slow or unreliable systems.</li>
 *   <li>Indicates a system-level issue, not a business or validation error.</li>
 *   <li>Typically mapped to HTTP 504 (Gateway Timeout) or 500 (Internal Server Error) in the web layer.</li>
 * </ul>
 *
 * <h2>Contextual information</h2>
 * <p>
 * Additional details about the operation (e.g., service name, timeout threshold)
 * should be logged via MDC or standard logging.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Use this exception for operations that fail due to timing out.</li>
 *   <li>Do not use it for business rule violations or validation errors.</li>
 * </ul>
 *
 * @see TechnicalException
 * @see AppException
 * @see ErrorCode
 */
public class TimeoutException extends TechnicalException {
    public TimeoutException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
