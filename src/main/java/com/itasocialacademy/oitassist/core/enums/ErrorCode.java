package com.itasocialacademy.oitassist.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents application-specific error codes used across all layers.
 *
 * <p>
 * Each {@code ErrorCode} is associated with an {@link ErrorCategory}, which
 * determines the type of error and is used for mapping to HTTP status codes in
 * the web layer.
 * </p>
 *
 * @see ErrorCategory
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    COMMON_INTERNAL_ERROR(ErrorCategory.TECHNICAL),
    FILE_UPLOAD_FAILED(ErrorCategory.TECHNICAL),
    FILE_DELETE_FAILED(ErrorCategory.TECHNICAL),
    FILE_LISTING_FAILED(ErrorCategory.TECHNICAL),
    PROVIDER_NOT_SUPPORTED(ErrorCategory.NOT_FOUND),
    FILE_ASSET_NOT_FOUND(ErrorCategory.NOT_FOUND),
    INVALID_FILE_PATH(ErrorCategory.TECHNICAL),
    COMMON_VALIDATION_FAILED(ErrorCategory.VALIDATION),
    FILE_VALIDATION_FAILED(ErrorCategory.VALIDATION),
    INVALID_ACTIVATION_TOKEN(ErrorCategory.VALIDATION),

    TOKEN_EXPIRE(ErrorCategory.AUTHENTICATION),
    UNSUPPORTED_TOKEN(ErrorCategory.AUTHENTICATION),
    NO_SUCH_ALGORITHM(ErrorCategory.TECHNICAL),
    INVALID_TOKEN(ErrorCategory.AUTHENTICATION),
    INVALID_TOKEN_TYPE(ErrorCategory.AUTHENTICATION),
    BAD_CREDENTIAL(ErrorCategory.AUTHENTICATION),
    INVALID_SIGNATURE(ErrorCategory.AUTHENTICATION),
    EMPTY_CLAIMS(ErrorCategory.AUTHENTICATION),
    USER_CONTEXT_MISSING(ErrorCategory.AUTHENTICATION),

    ENTITY_NOT_FOUND(ErrorCategory.NOT_FOUND),

    SUBMISSION_NOT_FOUND(ErrorCategory.NOT_FOUND),
    TOUR_NOT_IN_PROGRESS(ErrorCategory.CONFLICT),
    NOT_A_PARTICIPANT(ErrorCategory.AUTHORIZATION),

    TASK_NOT_FOUND(ErrorCategory.NOT_FOUND),
    TASK_ASSIGNMENT_NOT_FOUND(ErrorCategory.NOT_FOUND),
    TASK_ALREADY_COMPLETED(ErrorCategory.CONFLICT),
    TASK_ACCESS_RESTRICTED(ErrorCategory.AUTHORIZATION),
    TASK_DELETION_RESTRICTED(ErrorCategory.CONFLICT),
    TASK_ALREADY_ASSIGNED(ErrorCategory.CONFLICT),

    QUESTION_NOT_FOUND(ErrorCategory.NOT_FOUND),
    QUESTION_ACCESS_RESTRICTED(ErrorCategory.AUTHORIZATION),
    QUESTION_INVALID_STATE(ErrorCategory.CONFLICT),
    QUESTION_VERSION_CONFLICT(ErrorCategory.CONFLICT),

    USER_NOT_FOUND(ErrorCategory.NOT_FOUND),
    USER_ALREADY_EXISTS(ErrorCategory.CONFLICT),
    ACTIVATION_EMAIL_SENDING_TIMEOUT(ErrorCategory.VALIDATION),
    USER_ALREADY_ACTIVATED(ErrorCategory.CONFLICT),
    USER_NOT_ACTIVATED(ErrorCategory.CONFLICT),
    USER_BLOCKED(ErrorCategory.CONFLICT),
    ACCESS_DENIED(ErrorCategory.AUTHORIZATION),

    USER_SELF_CHANGE(ErrorCategory.AUTHORIZATION),
    ADMIN_MODIFICATION_RESTRICTED(ErrorCategory.AUTHORIZATION),
    AUTHENTICATION_REQUIRED(ErrorCategory.AUTHENTICATION),

    USER_PARTICIPATION_REQUEST_RESTRICTED(ErrorCategory.VALIDATION),
    REQUEST_CANNOT_BE_PROCESSED(ErrorCategory.VALIDATION),

    // Log files
    INVALID_LOG_FILE_PAGINATION(ErrorCategory.VALIDATION),
    LOG_FILE_LISTING_FAILED(ErrorCategory.TECHNICAL),
    INVALID_LOG_FILE_SORT(ErrorCategory.VALIDATION),

    DATA_ACCESS_ERROR(ErrorCategory.TECHNICAL);

    private final ErrorCategory category;
}
