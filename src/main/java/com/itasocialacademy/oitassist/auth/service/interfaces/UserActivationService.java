package com.itasocialacademy.oitassist.auth.service.interfaces;

import com.itasocialacademy.oitassist.auth.exceptions.UserAlreadyActivatedException;
import com.itasocialacademy.oitassist.user.exceptions.ActivationTokenSendingTimeoutException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;

/**
 * Application service responsible for user account activation workflows.
 * Provides operations related to activation email resending and activation
 * token generation. This service orchestrates activation-related use cases and
 * coordinates domain logic and persistence.
 */
public interface UserActivationService {
    /**
     * Resends the account activation email to a user whose account has not yet been
     * activated. If the current activation token is expired or missing, a new token
     * will be generated. If the resend request is performed before the allowed
     * timeout period, an exception is thrown.
     *
     * @param email the email address of the user
     *
     * @throws UserNotFoundException                  if no user exists with the
     *                                                provided email
     * @throws UserAlreadyActivatedException          if the user account is already
     *                                                activated
     * @throws ActivationTokenSendingTimeoutException if the resend request is made
     *                                                before the timeout expires
     */
    void resendVerificationEmail(String email);

    /**
     * Initializes the account activation process for a newly registered user.
     * Generates a new activation token, persists it to the user record, and sends a
     * verification email with an activation link.
     *
     * @param email     the email address of the user
     * @param firstName the first name of the user, used to personalize the email
     * @throws UserNotFoundException         if no user exists with the provided
     *                                       email
     * @throws UserAlreadyActivatedException if the user account is already
     *                                       activated
     */
    void initializeActivation(String email, String firstName);
}
