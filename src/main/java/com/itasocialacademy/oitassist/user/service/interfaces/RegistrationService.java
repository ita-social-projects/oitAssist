package com.itasocialacademy.oitassist.user.service.interfaces;

import com.itasocialacademy.oitassist.user.api.dto.RegisterCommand;

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
}
