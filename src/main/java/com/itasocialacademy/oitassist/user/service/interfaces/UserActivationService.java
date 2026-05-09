package com.itasocialacademy.oitassist.user.service.interfaces;

/**
 * Internal {@code user}-module service responsible for the activation lifecycle
 * of a user account: verifying email tokens, resending activation emails, and
 * initialising activation after registration.
 *
 * <p>
 * This interface is intentionally <strong>not</strong> annotated with
 * {@code @NamedInterface} — it is an internal implementation detail of the
 * {@code user} module. External modules interact with activation exclusively
 * through
 * {@link com.itasocialacademy.oitassist.user.api.interfaces.UserFacade}.
 * </p>
 */
public interface UserActivationService {
    /**
     * Activates a user account by consuming the given activation token. Transitions
     * the user from {@code PENDING} to {@code ACTIVE} and deletes the token so it
     * cannot be reused.
     *
     * @param token the raw activation token from the email link
     * @throws com.itasocialacademy.oitassist.user.exceptions.InvalidActivationTokenException if
     *                                                                                        the
     *                                                                                        token
     *                                                                                        does
     *                                                                                        not
     *                                                                                        exist
     *                                                                                        or
     *                                                                                        has
     *                                                                                        expired
     * @throws com.itasocialacademy.oitassist.user.exceptions.UserAlreadyActivatedException   if
     *                                                                                        the
     *                                                                                        account
     *                                                                                        is
     *                                                                                        already
     *                                                                                        active
     */
    void verifyEmail(String token);

    /**
     * Resends an activation email to a user whose account is still {@code PENDING}.
     * Generates a fresh token if the existing one has expired; enforces a resend
     * cooldown otherwise.
     *
     * @param email the email address of the pending user
     * @throws com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException                  if
     *                                                                                               no
     *                                                                                               user
     *                                                                                               exists
     *                                                                                               with
     *                                                                                               the
     *                                                                                               given
     *                                                                                               email
     * @throws com.itasocialacademy.oitassist.user.exceptions.UserAlreadyActivatedException          if
     *                                                                                               the
     *                                                                                               account
     *                                                                                               is
     *                                                                                               already
     *                                                                                               active
     * @throws com.itasocialacademy.oitassist.user.exceptions.ActivationTokenSendingTimeoutException if
     *                                                                                               the
     *                                                                                               resend
     *                                                                                               cooldown
     *                                                                                               has
     *                                                                                               not
     *                                                                                               yet
     *                                                                                               elapsed
     */
    void resendVerificationEmail(String email);

    /**
     * Initializes the activation flow for a newly registered user: generates an
     * activation token, associates it with the user, and publishes a
     * {@link com.itasocialacademy.oitassist.user.api.events.UserRegisteredEvent}
     * that triggers the verification email asynchronously.
     *
     * @param email     the email of the newly registered user
     * @param firstName the user's first name, carried in the event for email
     *                  personalisation
     */
    void initializeActivation(String email, String firstName);
}