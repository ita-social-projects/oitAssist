package com.itasocialacademy.oitassist.competition.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

/**
 * Thrown when a client-submitted {@code version} does not match the entity's
 * current version in the database — i.e. the entity was modified by someone
 * else since the client last read it.
 */
public class StaleEntityVersionException extends BusinessException {
    public static final String ERROR_MESSAGE =
        "Object of class [%s] with identifier [%s] was updated by another request.";

    public StaleEntityVersionException(Class<?> entityClass, Object identifier) {
        super(ERROR_MESSAGE
            .formatted(entityClass.getName(), identifier), ErrorCode.ENTITY_VERSION_CONFLICT);
    }
}
