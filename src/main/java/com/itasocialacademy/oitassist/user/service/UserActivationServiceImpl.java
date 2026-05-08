package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.user.api.events.UserRegisteredEvent;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.model.UserActivationToken;
import com.itasocialacademy.oitassist.user.dao.repository.UserActivationTokenRepository;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.InvalidActivationTokenException;
import com.itasocialacademy.oitassist.user.exceptions.UserAlreadyActivatedException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import com.itasocialacademy.oitassist.user.service.interfaces.UserActivationService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivationServiceImpl implements UserActivationService {
    private final UserRepository userRepository;
    private final UserActivationTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Looks up the token, validates it is not expired, transitions the user to
     * {@code ACTIVE}, and removes the token so it cannot be reused.
     * </p>
     */
    @Override
    @Transactional
    public void verifyEmail(String token) {
        UserActivationToken activationToken = tokenRepository.findByToken(token)
            .orElseThrow(InvalidActivationTokenException::new);

        if (activationToken.isExpired()) {
            throw new InvalidActivationTokenException();
        }

        User user = activationToken.getUser();

        if (user.getUserStatus() == UserStatus.ACTIVE) {
            throw new UserAlreadyActivatedException();
        }

        user.setUserStatus(UserStatus.ACTIVE);
        user.setUserActivationToken(null);
        userRepository.save(user);

        log.info("User account activated for email={}", user.getEmail());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the existing activation token is expired or missing, a fresh token is
     * generated and assigned to the user. If a valid token already exists but the
     * resend cooldown has not elapsed, an exception is thrown. The updated user
     * state is persisted before the activation email is dispatched.
     * </p>
     */
    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = findPendingUser(email);
        UserActivationToken token = prepareActivationToken(user);
        userRepository.save(user);
        publishActivationEvent(user.getEmail(), user.getFirstName(), token.getToken());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Generates a fresh activation token, associates it with the user, persists the
     * state, then publishes a {@link UserRegisteredEvent} that triggers the
     * verification email asynchronously after the transaction commits.
     * </p>
     */
    @Override
    @Transactional
    public void initializeActivation(String email, String firstName) {
        User user = findPendingUser(email);
        UserActivationToken token = UserActivationToken.generateActivationToken();
        user.setUserActivationToken(token);
        userRepository.save(user);
        publishActivationEvent(email, firstName, token.getToken());
    }

    /**
     * Publishes a {@link UserRegisteredEvent} that is consumed asynchronously by
     * {@code auth.listener.ActivationAccountListener} after the current transaction
     * commits.
     *
     * <p>
     * Using Spring's {@link ApplicationEventPublisher} here keeps the {@code user}
     * module decoupled from email infrastructure. The listener lives in
     * {@code auth} and depends only on {@code core::EmailService}.
     * </p>
     */
    private void publishActivationEvent(String email, String firstName, String token) {
        eventPublisher.publishEvent(new UserRegisteredEvent(email, firstName, token));
    }

    /**
     * Returns the current token if still valid, or generates a fresh one. Enforces
     * a 2-minute resend cooldown when an unexpired token exists.
     */
    private UserActivationToken prepareActivationToken(User user) {
        UserActivationToken token = user.getUserActivationToken();
        if (token == null || token.isExpired()) {
            token = UserActivationToken.generateActivationToken();
            user.setUserActivationToken(token);
            return token;
        }

        token.validateResendAllowed(Duration.ofMinutes(2));
        token.markSent();
        return token;
    }

    /**
     * Loads a user by email and asserts they are in {@code PENDING} status.
     *
     * @throws UserNotFoundException         if no user exists with the email
     * @throws UserAlreadyActivatedException if the account is already active
     */
    private User findPendingUser(String email) {
        User user = userRepository.findUserByEmail(email)
            .orElseThrow(UserNotFoundException::new);

        if (user.getUserStatus() != UserStatus.PENDING) {
            throw new UserAlreadyActivatedException();
        }

        return user;
    }
}
