package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Represents authorization failures.
 * <p>
 * Thrown when an authenticated user attempts to access a resource or perform an operation
 * for which they lack sufficient permissions or roles.
 * </p>
 *
 * <h2>Architectural role</h2>
 * <ul>
 *   <li>Thrown from the service or security layer.</li>
 *   <li>Signals that the caller is authenticated but not authorized.</li>
 *   <li>Typically mapped to HTTP 403 (Forbidden) in the web layer.</li>
 * </ul>
 *
 * <h2>Contextual information</h2>
 * <p>
 * Additional data like user ID, roles, or IP address should be captured via MDC or
 * an audit logger, not stored directly in the exception instance.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Use this exception when a user lacks required permissions for a specific action.</li>
 *   <li>Do not use it for authentication failures — use {@link AuthenticationException} instead.</li>
 * </ul>
 *
 * @see SecurityException
 * @see AppException
 * @see ErrorCode
 */
public class AuthorizationException extends SecurityException {
    public AuthorizationException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
