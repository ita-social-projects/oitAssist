package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Base class for all business-rule violations.
 * <p>
 * {@code BusinessException} represents an error that occurs when
 * a business invariant, rule, or constraint is violated.
 * These exceptions indicate <b>expected</b> and <b>recoverable</b>
 * failures from the perspective of the application.
 * </p>
 *
 * <h2>Examples</h2>
 * <ul>
 *   <li>Attempt to modify a completed task</li>
 *   <li>Operation not allowed in the current domain state</li>
 *   <li>Business condition not satisfied</li>
 * </ul>
 *
 * <h2>Architectural role</h2>
 * <ul>
 *   <li>Thrown from the <b>service (business)</b> layer.</li>
 *   <li>Part of the domain error contract.</li>
 *   <li>Does <b>not</b> represent technical failures or infrastructure problems.</li>
 * </ul>
 *
 * <h2>HTTP mapping</h2>
 * <p>
 * Business exceptions are typically mapped to client error HTTP statuses
 * such as {@code 400}, {@code 403}, or {@code 404} depending on the
 * associated {@link ErrorCode}.
 * </p>
 *
 * <p>
 * The mapping to HTTP status codes is handled in the web layer
 * (e.g. {@code @ControllerAdvice}) and must not be defined here.
 * </p>
 *
 * <h2>Usage guidelines</h2>
 * <ul>
 *   <li>Extend this class for domain-specific business errors.</li>
 *   <li>Use meaningful {@link ErrorCode}s to distinguish error scenarios.</li>
 *   <li>Do not use this class for validation or technical failures.</li>
 * </ul>
 *
 * @see AppException
 * @see ErrorCode
 */
public class BusinessException extends AppException {

    /**
     * Constructs a new business exception with the given message and error code.
     *
     * @param message   human-readable description of the business rule violation
     * @param errorCode application-specific error identifier
     */
    protected BusinessException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}