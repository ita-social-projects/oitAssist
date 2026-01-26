package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Represents authentication failures.
 * <p>
 * Thrown when a user or client fails to authenticate,
 * e.g., due to invalid credentials, missing token, or expired session.
 * </p>
 *
 * <h2>Architectural role</h2>
 * <ul>
 *   <li>Thrown from the security or service layer.</li>
 *   <li>Signals that the caller is unauthenticated.</li>
 *   <li>Mapped to HTTP 401 (Unauthorized) in the web layer.</li>
 * </ul>
 *
 * <h2>Contextual information</h2>
 * <p>
 * Additional data like user ID or IP address should be logged via MDC or an audit logger,
 * not stored directly in the exception.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Use this exception for failed login attempts or missing/invalid credentials.</li>
 *   <li>Do not use it for authorization failures — use {@link AuthorizationException} instead.</li>
 * </ul>
 *
 * @see SecurityException
 * @see AppException
 * @see ErrorCode
 */
public class AuthenticationException extends SecurityException {
    public AuthenticationException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
