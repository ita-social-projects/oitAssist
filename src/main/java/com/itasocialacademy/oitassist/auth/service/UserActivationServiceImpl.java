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
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = findNotActivatedUser(email);

        UserActivationToken token = prepareToken(user);

        userRepository.save(user);

        publishActivationEvent(user.getEmail(), user.getFirstName(), token.getToken());
    }

    @Override
    public void publishActivationEvent(String email, String firstName, String token) {
        eventPublisher.publishEvent(new ActivationAccountEvent(email, firstName, token));
    }

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

    private User findNotActivatedUser(String email) {
        User user = userRepository.findUserByEmail(email)
            .orElseThrow(UserNotFoundException::new);

        if (user.getUserStatus() != UserStatus.NOT_ACTIVATED) {
            throw new UserAlreadyActivatedException();
        }

        return user;
    }
}
