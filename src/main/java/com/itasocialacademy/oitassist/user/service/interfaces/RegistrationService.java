package com.itasocialacademy.oitassist.user.service.interfaces;

import com.itasocialacademy.oitassist.user.api.dto.OAuthProvisionCommand;
import com.itasocialacademy.oitassist.user.api.dto.RegisterCommand;
import com.itasocialacademy.oitassist.user.dao.model.User;

/**
 * Internal {@code user}-module service responsible for registering new user
 * accounts.
 *
 * <p>
 * This interface is intentionally <strong>not</strong> annotated with
 * {@code @NamedInterface} — it is an internal implementation detail of the
 * {@code user} module. External modules interact with registration exclusively
 * through
 * {@link com.itasocialacademy.oitassist.user.api.interfaces.UserFacade#register}.
 * </p>
 */
public interface RegistrationService {
    /**
     * * Creates a new user account in {@code PENDING} status, hashes the password,
     * and triggers the activation flow.
     *
     * @param command the registration data
     * @throws com.itasocialacademy.oitassist.user.exceptions.UserAlreadyExistsException if
     *                                                                                   an
     *                                                                                   active
     *                                                                                   account
     *                                                                                   with
     *                                                                                   the
     *                                                                                   given
     *                                                                                   email
     *                                                                                   already
     *                                                                                   exists
     * @throws com.itasocialacademy.oitassist.user.exceptions.UserNotActivatedException  if
     *                                                                                   a
     *                                                                                   pending
     *                                                                                   (not
     *                                                                                   yet
     *                                                                                   activated)
     *                                                                                   account
     *                                                                                   already
     *                                                                                   exists
     *                                                                                   for
     *                                                                                   the
     *                                                                                   given
     *                                                                                   email
     */
    void register(RegisterCommand command);

    /**
     * Provisions a user originating from a verified OAuth2 / OIDC identity.
     *
     * <p>
     * If a user already exists with {@code command.email()}, returns its
     * {@link UserAuthDetails} unchanged — no fields are updated, no provider
     * linkage is recorded. This is the auto-link behavior: an email previously
     * registered via password sign-in or via a different OAuth provider is silently
     * accepted as the same identity, since the calling module has verified email
     * ownership at the provider before invoking this method.
     * </p>
     *
     * <p>
     * If no user exists, creates one in {@code ACTIVE} status with
     * {@code Role.USER}, an unguessable random password hash, and the name fields
     * from the command. The new user is persisted and its {@link UserAuthDetails}
     * returned.
     * </p>
     *
     * @param command the verified OAuth2 identity; must not be null
     * @return the persisted or located {@link User} entity for the given identity
     */
    User provisionOAuthUser(OAuthProvisionCommand command);
}
