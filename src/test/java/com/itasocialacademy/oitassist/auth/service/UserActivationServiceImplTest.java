package com.itasocialacademy.oitassist.auth.service;

import com.itasocialacademy.oitassist.auth.dao.dto.event.ActivationAccountEvent;
import com.itasocialacademy.oitassist.auth.exceptions.UserAlreadyActivatedException;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.model.UserActivationToken;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.ActivationTokenSendingTimeoutException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserActivationServiceImplTest {
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserActivationServiceImpl userActivationService;

    @Test
    void resendVerificationEmail_userNotFound_shouldThrow() {
        String email = "test@mail.com";

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userActivationService.resendVerificationEmail(email));
    }

    @Test
    void resendVerificationEmail_userActivated_shouldThrow() {
        String email = "test@mail.com";
        User user = User.builder()
            .email(email)
            .userStatus(UserStatus.ACTIVATED)
            .build();

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyActivatedException.class, () -> userActivationService.resendVerificationEmail(email));
    }

    @Test
    void resendVerificationEmail_tokenNotFound_shouldCreateNewAndResendEmail() {
        String email = "test@mail.com";

        User user = User.builder()
            .email(email)
            .userStatus(UserStatus.NOT_ACTIVATED)
            .build();

        UserActivationToken userActivationToken = UserActivationToken.generateActivationToken();

        ActivationAccountEvent event =
            new ActivationAccountEvent(email, user.getFirstName(), userActivationToken.getToken());

        try (MockedStatic<UserActivationToken> mocked = mockStatic(UserActivationToken.class)) {
            when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));

            mocked.when(UserActivationToken::generateActivationToken).thenReturn(userActivationToken);

            doNothing().when(eventPublisher).publishEvent(event);

            userActivationService.resendVerificationEmail(email);

            verify(userRepository, times(1)).save(user);
            verify(eventPublisher, times(1)).publishEvent(event);
            assertNotNull(user.getUserActivationToken());
            assertEquals(userActivationToken.getToken(), user.getUserActivationToken().getToken());
        }
    }

    @Test
    void resendVerificationEmail_tokenExpired_shouldCreateNewAndResendEmail() {
        String email = "test@mail.com";

        UserActivationToken expiredToken = UserActivationToken.generateActivationToken();
        expiredToken.setExpiresAt(expiredToken.getCreatedAt().minusSeconds(1));

        UserActivationToken newToken = UserActivationToken.generateActivationToken();

        User user = User.builder()
            .email(email)
            .userActivationToken(expiredToken)
            .userStatus(UserStatus.NOT_ACTIVATED)
            .build();

        ActivationAccountEvent event = new ActivationAccountEvent(email, user.getFirstName(), newToken.getToken());

        try (MockedStatic<UserActivationToken> mocked = mockStatic(UserActivationToken.class)) {
            when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));

            mocked.when(UserActivationToken::generateActivationToken).thenReturn(newToken);

            doNothing().when(eventPublisher).publishEvent(event);

            userActivationService.resendVerificationEmail(email);

            verify(userRepository).save(user);
            assertNotNull(user.getUserActivationToken());
            assertEquals(newToken.getToken(), user.getUserActivationToken().getToken());
        }
    }

    @Test
    void resendVerificationEmail_resendTimeoutNotExceeded_shouldThrow() {
        String email = "test@mail.com";

        UserActivationToken activationToken = UserActivationToken.generateActivationToken();

        User user = User.builder()
            .email(email)
            .userActivationToken(activationToken)
            .userStatus(UserStatus.NOT_ACTIVATED)
            .build();

        try (MockedStatic<UserActivationToken> mocked = mockStatic(UserActivationToken.class)) {
            when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));

            mocked.when(UserActivationToken::generateActivationToken).thenReturn(activationToken);

            assertThrows(ActivationTokenSendingTimeoutException.class,
                () -> userActivationService.resendVerificationEmail(email));

            verify(userRepository, never()).save(user);
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Test
    void resendVerificationEmail_allConditionsPassed_shouldResend() {
        String email = "test@mail.com";
        String firstName = "Test";

        UserActivationToken activationToken = UserActivationToken.generateActivationToken();
        activationToken.setCreatedAt(activationToken.getCreatedAt().minusSeconds(600));
        activationToken.setLastSentAt(Instant.now().minusSeconds(300));

        User user = User.builder()
            .email(email)
            .firstName(firstName)
            .userActivationToken(activationToken)
            .userStatus(UserStatus.NOT_ACTIVATED)
            .build();

        ActivationAccountEvent event =
            new ActivationAccountEvent(user.getEmail(), user.getFirstName(), activationToken.getToken());

        try (MockedStatic<UserActivationToken> mocked = mockStatic(UserActivationToken.class)) {
            when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));

            mocked.when(UserActivationToken::generateActivationToken).thenReturn(activationToken);
            doNothing().when(eventPublisher).publishEvent(event);

            userActivationService.resendVerificationEmail(email);

            verify(userRepository, times(1)).save(user);
            verify(eventPublisher, times(1)).publishEvent(event);
        }
    }
}
