package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Represents a missing or required HTTP header in the request.
 * <p>
 * Thrown when the client request lacks a necessary header for processing.
 * </p>
 *
 * <h2>Architectural role</h2>
 * <ul>
 *   <li>Thrown from controllers, filters, or web layer components.</li>
 *   <li>Indicates a client-side error related to request formatting.</li>
 *   <li>Typically mapped to HTTP 400 (Bad Request) in the web layer.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Use this exception when required HTTP headers are missing from the request.</li>
 *   <li>Do not use it for business rule violations or system-level failures.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * if (!request.containsHeader("X-Custom-Header")) {
 *     throw new MissingHeaderException(
 *         "Required header X-Custom-Header is missing",
 *         ErrorCode.MISSING_HEADER
 *     );
 * }
 * }</pre>
 *
 * @see WebException
 * @see AppException
 * @see ErrorCode
 */
public class MissingHeaderException extends WebException {
    public MissingHeaderException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
