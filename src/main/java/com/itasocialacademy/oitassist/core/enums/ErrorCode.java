package com.itasocialacademy.oitassist.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents application-specific error codes used across all layers.
 * <p>
 * Each {@code ErrorCode} is associated with an {@link ErrorCategory}, which determines
 * the type of error and is used for mapping to HTTP status codes in the web layer.
 * </p>
 *
 * @see ErrorCategory
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    COMMON_INTERNAL_ERROR(ErrorCategory.TECHNICAL),
    COMMON_VALIDATION_FAILED(ErrorCategory.VALIDATION),

    TASK_NOT_FOUND(ErrorCategory.NOT_FOUND),
    TASK_ALREADY_COMPLETED(ErrorCategory.CONFLICT),
    TASK_ACCESS_RESTRICTED(ErrorCategory.AUTHORIZATION),

    USER_NOT_FOUND(ErrorCategory.NOT_FOUND),
    USER_ALREADY_EXISTS(ErrorCategory.CONFLICT);

    private final ErrorCategory category;
}
