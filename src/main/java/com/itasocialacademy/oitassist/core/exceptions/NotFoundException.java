package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Represents a domain-level "not found" condition.
 * <p>
 * Thrown when a requested resource, entity, or object does not exist
 * in the system according to business rules.
 * </p>
 *
 * <h2>Architectural role</h2>
 * <ul>
 *   <li>Thrown from service or repository layers when a domain object cannot be found.</li>
 *   <li>Indicates a client error: the requested operation cannot be completed
 *       because the resource does not exist.</li>
 *   <li>Typically mapped to HTTP 404 (Not Found) in the web layer.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Use this exception for missing domain entities (e.g., Task, User).</li>
 *   <li>Do not use it for technical failures or validation errors.</li>
 * </ul>
 *
 * @see BusinessException
 * @see AppException
 * @see ErrorCode
 */
public class NotFoundException extends BusinessException {
    public NotFoundException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
