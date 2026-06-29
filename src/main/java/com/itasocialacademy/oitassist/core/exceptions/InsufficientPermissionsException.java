package com.itasocialacademy.oitassist.core.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;

/**
 * Thrown when a user attempts to perform an action without sufficient
 * permissions according to business rules.
 *
 * <p>
 * This exception should be used for domain-level access restrictions, such as
 * attempting to modify protected resources or roles.
 * </p>
 */
public class InsufficientPermissionsException extends BusinessException {
    public InsufficientPermissionsException() {
        super("You do not have enough permissions to perform this action", ErrorCode.ACCESS_DENIED);
    }
}