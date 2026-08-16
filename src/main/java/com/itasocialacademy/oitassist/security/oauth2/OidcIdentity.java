package com.itasocialacademy.oitassist.security.oauth2;

/**
 * Security-module-internal carrier for the identity attributes extracted from
 * an OIDC principal. Deliberately not a cross-module DTO — exists only to pass
 * data from {@link OAuth2UserAttributeExtractor} to
 * {@link OAuth2SuccessHandler} without introducing a {@code user}-owned type
 * into {@code security}. See {@code OAuthUserProvisioningPort} for why that
 * matters.
 */
record OidcIdentity(String email, String firstName, String surname) {
}