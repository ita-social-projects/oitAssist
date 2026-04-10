package com.itasocialacademy.oitassist.auth.service;

import com.itasocialacademy.oitassist.auth.dto.event.ActivationAccountEvent;
import com.itasocialacademy.oitassist.auth.exceptions.UserAlreadyActivatedException;
import com.itasocialacademy.oitassist.auth.service.interfaces.UserActivationService;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.model.UserActivationToken;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserActivationServiceImpl implements UserActivationService {
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

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
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = findNotActivatedUser(email);

        UserActivationToken token = prepareToken(user);

        userRepository.save(user);

        sendActivationEmail(user.getEmail(), user.getFirstName(), token.getToken());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void initializeActivation(String email, String firstName) {
        User user = findNotActivatedUser(email);
        UserActivationToken token = UserActivationToken.generateActivationToken();
        user.setUserActivationToken(token);
        userRepository.save(user);
        sendActivationEmail(email, firstName, token.getToken());
    }

    /**
     * Publishes an {@link ActivationAccountEvent} that is handled asynchronously by
     * {@link com.itasocialacademy.oitassist.auth.listener.ActivationAccountListener},
     * which builds the activation link and sends the verification email.
     */
    private void sendActivationEmail(String email, String firstName, String token) {
        eventPublisher.publishEvent(new ActivationAccountEvent(email, firstName, token));
    }

    /**
     * Returns the current token if it is still valid, or generates a fresh one.
     * Enforces a resend cooldown when an unexpired token already exists.
     */
    private UserActivationToken prepareToken(User user) {
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
     * Loads the user by email and ensures their account is in {@code NOT_ACTIVATED}
     * status.
     *
     * @throws UserNotFoundException         if no user exists with the provided
     *                                       email
     * @throws UserAlreadyActivatedException if the account is already active
     */
    private User findNotActivatedUser(String email) {
        User user = userRepository.findUserByEmail(email)
            .orElseThrow(UserNotFoundException::new);

        if (user.getUserStatus() != UserStatus.NOT_ACTIVATED) {
            throw new UserAlreadyActivatedException();
        }

        return user;
    }
}
