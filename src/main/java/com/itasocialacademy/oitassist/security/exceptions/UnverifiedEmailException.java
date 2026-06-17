package com.itasocialacademy.oitassist.security.exceptions;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Thrown when the OAuth2 provider has not verified the email address, or when
 * the email claim is absent entirely.
 *
 * <p>
 * Extends {@link OAuth2AuthenticationException} so Spring Security's OAuth2
 * filter chain recognises it as an authentication failure and routes it to the
 * configured {@code AuthenticationFailureHandler}.
 * </p>
 */
public class UnverifiedEmailException extends OAuth2AuthenticationException {
    private static final String ERROR_CODE = "email_not_verified";

    public UnverifiedEmailException() {
        super(new OAuth2Error(ERROR_CODE), "OAuth2 provider has not verified the email address");
    }
}