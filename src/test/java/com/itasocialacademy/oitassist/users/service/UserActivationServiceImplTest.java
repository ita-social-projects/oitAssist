package com.itasocialacademy.oitassist.users.service;

import com.itasocialacademy.oitassist.user.api.events.UserRegisteredEvent;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.model.UserActivationToken;
import com.itasocialacademy.oitassist.user.dao.repository.UserActivationTokenRepository;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.ActivationTokenSendingTimeoutException;
import com.itasocialacademy.oitassist.user.exceptions.InvalidActivationTokenException;
import com.itasocialacademy.oitassist.user.exceptions.UserAlreadyActivatedException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import com.itasocialacademy.oitassist.user.service.UserActivationServiceImpl;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit test for UserActivationServiceImpl")
class UserActivationServiceImplTest {
    private static final String TOKEN = "activation-token";
    private static final String EMAIL = "ivan@example.com";
    private static final String FIRST_NAME = "Ivan";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserActivationTokenRepository tokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserActivationToken activationToken;

    @InjectMocks
    private UserActivationServiceImpl activationService;

    @Test
    @DisplayName("verifyEmail should throw InvalidActivationTokenException when token not found")
    void verifyEmail_ShouldThrowInvalidActivationTokenException_WhenTokenNotFound() {
        when(tokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activationService.verifyEmail(TOKEN))
            .isInstanceOf(InvalidActivationTokenException.class);

        verifyNoInteractions(userRepository, eventPublisher);
    }

    @Test
    @DisplayName("verifyEmail should throw InvalidActivationTokenException when token is expired")
    void verifyEmail_ShouldThrowInvalidActivationTokenException_WhenTokenIsExpired() {
        when(tokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(activationToken));
        when(activationToken.isExpired()).thenReturn(true);

        assertThatThrownBy(() -> activationService.verifyEmail(TOKEN))
            .isInstanceOf(InvalidActivationTokenException.class);

        verifyNoInteractions(userRepository, eventPublisher);
    }

    @Test
    @DisplayName("verifyEmail should throw UserAlreadyActivatedException when user is already active")
    void verifyEmail_ShouldThrowUserAlreadyActivatedException_WhenUserIsAlreadyActive() {
        User user = User.builder()
            .email(EMAIL)
            .userStatus(UserStatus.ACTIVE)
            .build();

        when(tokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(activationToken));
        when(activationToken.isExpired()).thenReturn(false);
        when(activationToken.getUser()).thenReturn(user);

        assertThatThrownBy(() -> activationService.verifyEmail(TOKEN))
            .isInstanceOf(UserAlreadyActivatedException.class);

        verifyNoInteractions(userRepository, eventPublisher);
    }

    @Test
    @DisplayName("verifyEmail should activate user and remove token when token is valid")
    void verifyEmail_ShouldActivateUserAndRemoveToken_WhenTokenIsValid() {
        User user = User.builder()
            .email(EMAIL)
            .userStatus(UserStatus.PENDING)
            .userActivationToken(activationToken)
            .build();

        when(tokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(activationToken));
        when(activationToken.isExpired()).thenReturn(false);
        when(activationToken.getUser()).thenReturn(user);

        activationService.verifyEmail(TOKEN);

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getUserActivationToken()).isNull();

        verify(userRepository).save(user);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("initializeActivation should throw UserNotFoundException when user not found")
    void initializeActivation_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activationService.initializeActivation(EMAIL, FIRST_NAME))
            .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findUserByEmail(EMAIL);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(tokenRepository, eventPublisher);
    }

    @Test
    @DisplayName("initializeActivation should throw UserAlreadyActivatedException when user is not pending")
    void initializeActivation_ShouldThrowUserAlreadyActivatedException_WhenUserIsNotPending() {
        User user = User.builder()
            .email(EMAIL)
            .userStatus(UserStatus.ACTIVE)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> activationService.initializeActivation(EMAIL, FIRST_NAME))
            .isInstanceOf(UserAlreadyActivatedException.class);

        verify(userRepository).findUserByEmail(EMAIL);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(tokenRepository, eventPublisher);
    }

    @Test
    @DisplayName("initializeActivation should assign new token, save user and publish event when user is pending")
    void initializeActivation_ShouldAssignTokenAndPublishEvent_WhenUserIsPending() {
        User user = User.builder()
            .email(EMAIL)
            .userStatus(UserStatus.PENDING)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));

        activationService.initializeActivation(EMAIL, FIRST_NAME);

        UserActivationToken assignedToken = user.getUserActivationToken();
        assertThat(assignedToken).isNotNull();
        assertThat(assignedToken.getToken()).isNotBlank();
        assertThat(assignedToken.getUser()).isSameAs(user);

        verify(userRepository).save(user);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserRegisteredEvent event = eventCaptor.getValue();
        assertThat(event.email()).isEqualTo(EMAIL);
        assertThat(event.firstName()).isEqualTo(FIRST_NAME);
        assertThat(event.token()).isEqualTo(assignedToken.getToken());
    }

    @Test
    @DisplayName("resendVerificationEmail should generate new token when user has no token")
    void resendVerificationEmail_ShouldGenerateNewToken_WhenUserHasNoToken() {
        User user = User.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .userStatus(UserStatus.PENDING)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));

        activationService.resendVerificationEmail(EMAIL);

        UserActivationToken newToken = user.getUserActivationToken();
        assertThat(newToken).isNotNull();
        assertThat(newToken.getToken()).isNotBlank();

        verify(userRepository).save(user);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserRegisteredEvent event = eventCaptor.getValue();
        assertThat(event.email()).isEqualTo(EMAIL);
        assertThat(event.firstName()).isEqualTo(FIRST_NAME);
        assertThat(event.token()).isEqualTo(newToken.getToken());
    }

    @Test
    @DisplayName("resendVerificationEmail should regenerate token when existing token is expired")
    void resendVerificationEmail_ShouldRegenerateToken_WhenTokenIsExpired() {
        UserActivationToken expiredToken = UserActivationToken.builder()
            .token("old-token")
            .expiresAt(Instant.now().minusSeconds(60))
            .lastSentAt(Instant.now().minusSeconds(20 * 60))
            .build();

        User user = User.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .userStatus(UserStatus.PENDING)
            .userActivationToken(expiredToken)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));

        activationService.resendVerificationEmail(EMAIL);

        assertThat(user.getUserActivationToken()).isSameAs(expiredToken);
        assertThat(expiredToken.getToken()).isNotEqualTo("old-token");
        assertThat(expiredToken.getExpiresAt()).isAfter(Instant.now());

        verify(userRepository).save(user);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().token()).isEqualTo(expiredToken.getToken());
    }

    @Test
    @DisplayName("resendVerificationEmail should resend existing token when token is valid and cooldown has passed")
    void resendVerificationEmail_ShouldResendExistingToken_WhenTokenIsValidAndCooldownPassed() {
        Instant previousSentAt = Instant.now().minusSeconds(5 * 60);

        UserActivationToken validToken = UserActivationToken.builder()
            .token("valid-token")
            .expiresAt(Instant.now().plusSeconds(10 * 60))
            .lastSentAt(previousSentAt)
            .build();

        User user = User.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .userStatus(UserStatus.PENDING)
            .userActivationToken(validToken)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));

        activationService.resendVerificationEmail(EMAIL);

        assertThat(validToken.getToken()).isEqualTo("valid-token");
        assertThat(validToken.getLastSentAt()).isAfter(previousSentAt);

        verify(userRepository).save(user);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().token()).isEqualTo("valid-token");
    }

    @Test
    @DisplayName("resendVerificationEmail should throw timeout exception when cooldown has not passed")
    void resendVerificationEmail_ShouldThrowTimeoutException_WhenCooldownHasNotPassed() {
        UserActivationToken recentlySentToken = UserActivationToken.builder()
            .token("valid-token")
            .expiresAt(Instant.now().plusSeconds(10 * 60))
            .lastSentAt(Instant.now().minusSeconds(30))
            .build();

        User user = User.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .userStatus(UserStatus.PENDING)
            .userActivationToken(recentlySentToken)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> activationService.resendVerificationEmail(EMAIL))
            .isInstanceOf(ActivationTokenSendingTimeoutException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }
}