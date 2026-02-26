package com.itasocialacademy.oitassist.auth.service;

import com.itasocialacademy.oitassist.auth.AuthTestDataFactory;
import com.itasocialacademy.oitassist.auth.dao.dto.event.ActivationAccountEvent;
import com.itasocialacademy.oitassist.auth.dao.dto.request.ResendVerificationMailRequest;
import com.itasocialacademy.oitassist.auth.exceptions.UserAlreadyActivatedException;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.model.UserActivationToken;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.ActivationTokenSendingTimeoutException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for User Activation Service")
class UserActivationServiceImplTest {
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserActivationServiceImpl userActivationService;

    @Test
    @DisplayName("User not found")
    void resendVerificationEmail_userNotFound_shouldThrow() {
        // given
        ResendVerificationMailRequest request = AuthTestDataFactory.validResendVerificationMailRequest();
        String email = request.getEmail();

        // when
        when(userRepository.findUserByEmail(email)).thenReturn(Optional.empty());

        // then
        assertThrows(UserNotFoundException.class,
            () -> userActivationService.resendVerificationEmail(email));
        verify(userRepository, times(1)).findUserByEmail(email);
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("User already activated")
    void resendVerificationEmail_userActivated_shouldThrow() {
        // given
        User user = mock(User.class);
        String email = user.getEmail();
        ResendVerificationMailRequest request = AuthTestDataFactory.validResendVerificationMailRequest();

        // when
        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(user.getUserStatus()).thenReturn(UserStatus.ACTIVATED);

        // then
        assertThrows(UserAlreadyActivatedException.class,
            () -> userActivationService.resendVerificationEmail(email));
        verify(userRepository, times(1)).findUserByEmail(email);
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("User not activated and activation token not found")
    void resendVerificationEmail_tokenNotFound_shouldCreateNewAndResendEmail() {
        // given
        ResendVerificationMailRequest request = AuthTestDataFactory.validResendVerificationMailRequest();

        User user = User.builder()
            .email(request.getEmail())
            .userStatus(UserStatus.NOT_ACTIVATED)
            .build();

        UserActivationToken userActivationToken = UserActivationToken.generateActivationToken();

        ActivationAccountEvent event =
            new ActivationAccountEvent(request.getEmail(), user.getFirstName(), userActivationToken.getToken());

        try (MockedStatic<UserActivationToken> mocked = mockStatic(UserActivationToken.class)) {
            // when
            when(userRepository.findUserByEmail(request.getEmail())).thenReturn(Optional.of(user));
            mocked.when(UserActivationToken::generateActivationToken).thenReturn(userActivationToken);
            doNothing().when(eventPublisher).publishEvent(event);
            userActivationService.resendVerificationEmail(request.getEmail());

            // then
            verify(userRepository, times(1)).findUserByEmail(request.getEmail());
            verify(userRepository, times(1)).save(user);
            verify(eventPublisher, times(1)).publishEvent(event);
            assertNotNull(user.getUserActivationToken());
            assertEquals(userActivationToken.getToken(), user.getUserActivationToken().getToken());
        }
    }

    @Test
    @DisplayName("User not activated and activation token expired")
    void resendVerificationEmail_tokenExpired_shouldCreateNewAndResendEmail() {
        // given
        ResendVerificationMailRequest request = AuthTestDataFactory.validResendVerificationMailRequest();
        UserActivationToken expiredToken = UserActivationToken.generateActivationToken();
        expiredToken.setExpiresAt(expiredToken.getCreatedAt().minusSeconds(1));

        UserActivationToken newToken = UserActivationToken.generateActivationToken();

        User user = User.builder()
            .email(request.getEmail())
            .userActivationToken(expiredToken)
            .userStatus(UserStatus.NOT_ACTIVATED)
            .build();

        ActivationAccountEvent event =
            new ActivationAccountEvent(request.getEmail(), user.getFirstName(), newToken.getToken());

        try (MockedStatic<UserActivationToken> mocked = mockStatic(UserActivationToken.class)) {
            // when
            when(userRepository.findUserByEmail(request.getEmail())).thenReturn(Optional.of(user));
            mocked.when(UserActivationToken::generateActivationToken).thenReturn(newToken);
            doNothing().when(eventPublisher).publishEvent(event);

            userActivationService.resendVerificationEmail(request.getEmail());

            // then
            verify(userRepository, times(1)).findUserByEmail(request.getEmail());
            verify(userRepository).save(user);
            verify(eventPublisher, times(1)).publishEvent(event);
            assertNotNull(user.getUserActivationToken());
            assertEquals(newToken.getToken(), user.getUserActivationToken().getToken());
        }
    }

    @Test
    @DisplayName("User not activated and activation token resend timeout not exceeded")
    void resendVerificationEmail_resendTimeoutNotExceeded_shouldThrow() {
        // given
        ResendVerificationMailRequest request = AuthTestDataFactory.validResendVerificationMailRequest();
        String email = request.getEmail();
        UserActivationToken activationToken = UserActivationToken.generateActivationToken();

        User user = User.builder()
            .email(email)
            .userActivationToken(activationToken)
            .userStatus(UserStatus.NOT_ACTIVATED)
            .build();

        try (MockedStatic<UserActivationToken> mocked = mockStatic(UserActivationToken.class)) {
            // when
            when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));
            mocked.when(UserActivationToken::generateActivationToken).thenReturn(activationToken);

            // then
            assertThrows(ActivationTokenSendingTimeoutException.class,
                () -> userActivationService.resendVerificationEmail(email));
            verify(userRepository, times(1)).findUserByEmail(email);
            verify(userRepository, never()).save(user);
            verify(eventPublisher, never()).publishEvent(any());
            assertNotNull(user.getUserActivationToken());
            assertEquals(activationToken.getToken(), user.getUserActivationToken().getToken());
        }
    }

    @Test
    @DisplayName("All conditions passed")
    void resendVerificationEmail_allConditionsPassed_shouldResend() {
        // given
        String firstName = "John";
        ResendVerificationMailRequest request = AuthTestDataFactory.validResendVerificationMailRequest();
        UserActivationToken activationToken = UserActivationToken.generateActivationToken();
        activationToken.setCreatedAt(activationToken.getCreatedAt().minusSeconds(600));
        activationToken.setLastSentAt(Instant.now().minusSeconds(300));

        User user = User.builder()
            .email(request.getEmail())
            .firstName(firstName)
            .userActivationToken(activationToken)
            .userStatus(UserStatus.NOT_ACTIVATED)
            .build();

        ActivationAccountEvent event =
            new ActivationAccountEvent(user.getEmail(), user.getFirstName(), activationToken.getToken());

        try (MockedStatic<UserActivationToken> mocked = mockStatic(UserActivationToken.class)) {
            // when
            when(userRepository.findUserByEmail(request.getEmail())).thenReturn(Optional.of(user));
            mocked.when(UserActivationToken::generateActivationToken).thenReturn(activationToken);
            doNothing().when(eventPublisher).publishEvent(event);

            userActivationService.resendVerificationEmail(request.getEmail());
            // then
            verify(userRepository, times(1)).findUserByEmail(request.getEmail());
            verify(userRepository, times(1)).save(user);
            verify(eventPublisher, times(1)).publishEvent(event);
            assertNotNull(user.getUserActivationToken());
            assertEquals(activationToken.getToken(), user.getUserActivationToken().getToken());
        }
    }
}
