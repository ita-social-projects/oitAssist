package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Represents a generic HTTP 400 (Bad Request) error.
 * <p>
 * Thrown when the client sends a request that is invalid, malformed,
 * or cannot be processed by the server due to request content issues.
 * </p>
 *
 * <h2>Architectural role</h2>
 * <ul>
 *   <li>Thrown from controllers, filters, or web layer components.</li>
 *   <li>Indicates a client-side error, not a system failure or business rule violation.</li>
 *   <li>Typically mapped to HTTP 400 (Bad Request) in API responses.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Use this exception when request parameters, headers, or body are invalid.</li>
 *   <li>Do not use it for business rule violations — use BusinessException or its subclasses instead.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * if (inputDto.getValue() < 0) {
 *     throw new BadRequestException(
 *         "Value must be positive",
 *         ErrorCode.INVALID_REQUEST_PARAM
 *     );
 * }
 * }</pre>
 *
 * @see WebException
 * @see AppException
 * @see ErrorCode
 */
public class BadRequestException extends WebException {
    public BadRequestException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
