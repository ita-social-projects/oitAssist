package com.itasocialacademy.oitassist.core.enums;

/**
 * Represents the high-level category of an {@link com.itasocialacademy.oitassist.core.enums.ErrorCode}.
 * <p>
 * Each category is used to classify errors and map them to appropriate HTTP status codes
 * in the web layer via {@link com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper}.
 * </p>
 *
 * @see ErrorCode
 * @see com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper
 */
public enum ErrorCategory {
    VALIDATION,
    AUTHORIZATION,
    AUTHENTICATION,
    NOT_FOUND,
    CONFLICT,
    TECHNICAL
}