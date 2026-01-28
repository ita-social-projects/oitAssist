package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Represents security-related violations such as authentication and
 * authorization failures.
 * <p>
 * {@code SecurityException} indicates that an operation was rejected due to
 * insufficient permissions or invalid security context.
 * </p>
 *
 * <h2>Architectural role</h2>
 * <ul>
 * <li>May be thrown from service, controller, or security layers.</li>
 * <li>Represents access control and authorization decisions.</li>
 * <li>Independent from business rule validation.</li>
 * </ul>
 *
 * <h2>Security context</h2>
 * <p>
 * Additional contextual information (e.g. user ID, IP address) must be captured
 * via logging or auditing mechanisms (e.g. MDC), not stored directly in the
 * exception instance.
 * </p>
 *
 * @see AppException
 * @see ErrorCode
 */
public class SecurityException extends AppException {
    protected SecurityException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
