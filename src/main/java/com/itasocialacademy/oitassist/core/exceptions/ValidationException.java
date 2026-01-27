package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Represents validation failures related to business and domain rules.
 * <p>
 * {@code ValidationException} is used when input data is structurally valid but
 * violates business-level constraints or invariants.
 * </p>
 *
 * <h2>Important distinction</h2>
 * <p>
 * This exception is <b>not</b> intended for HTTP or request-level validation
 * (e.g. {@code @Valid}, missing headers, malformed parameters). Such errors
 * must be handled at the web layer and represented by {@link WebException}
 * subclasses.
 * </p>
 *
 * <h2>Examples</h2>
 * <ul>
 * <li>Attempt to modify a completed task</li>
 * <li>Invalid domain state transition</li>
 * <li>Date range violates business rules</li>
 * <li>Operation not allowed due to current entity state</li>
 * </ul>
 *
 * <h2>Architectural role</h2>
 * <ul>
 * <li>Thrown from the <b>service (business)</b> layer.</li>
 * <li>Represents <b>expected</b> validation failures.</li>
 * <li>Indicates a client error after request-level validation passed.</li>
 * </ul>
 *
 * <h2>HTTP mapping</h2>
 * <p>
 * Typically mapped to {@code 400 Bad Request}, but the exact status code is
 * determined by the associated {@link ErrorCode}.
 * </p>
 *
 * <h2>Usage guidelines</h2>
 * <ul>
 * <li>Use this exception for domain validation, not transport validation.</li>
 * <li>Do not throw this exception from controllers or filters.</li>
 * <li>Prefer a specific {@link ErrorCode} over creating many subclasses.</li>
 * </ul>
 *
 * @see BusinessException
 * @see WebException
 * @see ErrorCode
 */
public class ValidationException extends BusinessException {

    /**
     * Constructs a new domain validation exception.
     *
     * @param message   human-readable description of the validation failure
     * @param errorCode application-specific error identifier
     */
    public ValidationException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
