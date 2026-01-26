package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Represents technical failures occurring while interacting with external services.
 * <p>
 * Thrown when the application cannot successfully communicate with an external system,
 * such as a REST API, messaging service, or third-party SDK.
 * </p>
 *
 * <h2>Architectural role</h2>
 * <ul>
 *   <li>Thrown from service or adapter layers that call external systems.</li>
 *   <li>Indicates a system-level issue, not a business or client error.</li>
 *   <li>Typically mapped to HTTP 500 (Internal Server Error) in API responses.</li>
 * </ul>
 *
 * <h2>Contextual information</h2>
 * <p>
 * Details about the underlying cause (HTTP status, timeout, exception from SDK)
 * can be included via the {@link Throwable#getCause()} mechanism.
 * Additional request/response context should be logged via MDC or audit logs.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Use this exception to wrap failures when calling external services.</li>
 *   <li>Do not use this exception for business rule violations or validation errors.</li>
 * </ul>
 *
 * @see TechnicalException
 * @see AppException
 * @see ErrorCode
 */
public class ExternalServiceException extends TechnicalException {
    public ExternalServiceException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
