package com.itasocialacademy.oitassist.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.springframework.modulith.NamedInterface;

/**
 * Cross-module command DTO used by external modules (currently
 * {@code security}) to look up or provision a user from a verified OAuth2 /
 * OpenID Connect identity.
 *
 * <p>
 * Distinct from {@link RegisterCommand} on three axes:
 * </p>
 * <ul>
 * <li><b>No password.</b> The caller has authenticated the user against an
 * external identity provider; the {@code user} module assigns an unguessable
 * random password hash internally so the {@code password} column constraint is
 * satisfied without exposing a usable credential.</li>
 * <li><b>Optional name fields.</b> Surname and middle name are not guaranteed
 * by every OIDC provider. {@code firstName} stays required because both
 * supported providers (Google {@code given_name}, Microsoft {@code givenname})
 * always supply it, and the caller falls back to the email local-part if they
 * ever don't.</li>
 * <li><b>Email is pre-verified.</b> The provider has already confirmed
 * ownership; the resulting account is created in {@code ACTIVE} status,
 * skipping the activation token flow entirely.</li>
 * </ul>
 *
 * <p>
 * Validation annotations declared here form the canonical contract. The caller
 * in {@code security} is responsible for constructing this command with values
 * consistent with these constraints — e.g. only after confirming
 * {@code email_verified} is true on the OIDC user.
 * </p>
 */
@Builder
@NamedInterface("OAuthProvisionCommand")
public record OAuthProvisionCommand(
    @NotBlank @Email String email,
    @NotBlank String firstName,
    String surname,
    String middleName,
    String phoneNumber) {
}
