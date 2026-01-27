package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Base class for web-layer and request-related exceptions.
 * <p>
 * {@code WebException} represents errors that occur at the API / transport
 * level and are caused by an invalid, incomplete, or unauthorized client
 * request. These errors happen <b>before</b> or <b>outside</b> of business
 * logic execution.
 * </p>
 *
 * <h2>Examples</h2>
 * <ul>
 * <li>Missing or invalid HTTP headers</li>
 * <li>Malformed request parameters</li>
 * <li>Authentication or authorization failures</li>
 * <li>Request validation errors at controller or filter level</li>
 * </ul>
 *
 * <h2>Architectural role</h2>
 * <ul>
 * <li>Thrown from the <b>web layer</b> (controllers, filters,
 * interceptors).</li>
 * <li>Represents client-side or protocol-level errors.</li>
 * <li>Must not contain or depend on domain-specific business rules.</li>
 * </ul>
 *
 * <h2>HTTP mapping</h2>
 * <p>
 * Web exceptions are typically mapped to {@code 4xx} HTTP status codes such as
 * {@code 400}, {@code 401}, {@code 403}, or {@code 404}, depending on the
 * associated {@link ErrorCode}.
 * </p>
 *
 * <p>
 * Mapping to HTTP response status codes is handled in the web layer (e.g.
 * {@code @ControllerAdvice}) and must not be defined here.
 * </p>
 *
 * <h2>Usage guidelines</h2>
 * <ul>
 * <li>Extend this class for all request-level and API-related errors.</li>
 * <li>Do not throw this exception from business or domain services.</li>
 * <li>Prefer business exceptions once request data has been accepted as
 * valid.</li>
 * </ul>
 *
 * @see AppException
 * @see BusinessException
 * @see TechnicalException
 * @see ErrorCode
 */
public class WebException extends AppException {

    /**
     * Constructs a new web exception with the given message and error code.
     *
     * @param message   human-readable description of the request-related error
     * @param errorCode application-specific error identifier
     */
    protected WebException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
