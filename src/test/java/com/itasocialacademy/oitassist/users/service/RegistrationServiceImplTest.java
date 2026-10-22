package com.itasocialacademy.oitassist.users.service;

import com.itasocialacademy.oitassist.user.api.dto.OAuthProvisionCommand;
import com.itasocialacademy.oitassist.user.api.dto.RegisterCommand;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.model.UserActivationToken;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.UserAlreadyExistsException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotActivatedException;
import com.itasocialacademy.oitassist.user.mapper.OAuthProvisionCommandMapper;
import com.itasocialacademy.oitassist.user.mapper.RegisterCommandMapper;
import com.itasocialacademy.oitassist.user.service.RandomPasswordGenerator;
import com.itasocialacademy.oitassist.user.service.RegistrationServiceImpl;
import com.itasocialacademy.oitassist.user.service.interfaces.UserActivationService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit test for RegistrationServiceImpl")
class RegistrationServiceImplTest {
    private static final String EMAIL = "ivan@example.com";
    private static final String FIRST_NAME = "Ivan";
    private static final String RAW_PASSWORD = "raw-password";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String RANDOM_PASSWORD = "random-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegisterCommandMapper registerCommandMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserActivationService userActivationService;

    @Mock
    private OAuthProvisionCommandMapper oauthProvisionCommandMapper;

    @Mock
    private RandomPasswordGenerator randomPasswordGenerator;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    @DisplayName("register should throw UserNotActivatedException when pending user with email exists")
    void register_ShouldThrowUserNotActivatedException_WhenPendingUserExists() {
        RegisterCommand command = RegisterCommand.builder()
            .email(EMAIL)
            .password(RAW_PASSWORD)
            .firstName(FIRST_NAME)
            .build();

        User existingUser = User.builder()
            .email(EMAIL)
            .userStatus(UserStatus.PENDING)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> registrationService.register(command))
            .isInstanceOf(UserNotActivatedException.class);

        verify(userRepository).findUserByEmail(EMAIL);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(registerCommandMapper, passwordEncoder, userActivationService);
    }

    @Test
    @DisplayName("register should throw UserAlreadyExistsException when active user with email exists")
    void register_ShouldThrowUserAlreadyExistsException_WhenActiveUserExists() {
        RegisterCommand command = RegisterCommand.builder()
            .email(EMAIL)
            .password(RAW_PASSWORD)
            .firstName(FIRST_NAME)
            .build();

        User existingUser = User.builder()
            .email(EMAIL)
            .userStatus(UserStatus.ACTIVE)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> registrationService.register(command))
            .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository).findUserByEmail(EMAIL);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(registerCommandMapper, passwordEncoder, userActivationService);
    }

    @Test
    @DisplayName("register should throw UserAlreadyExistsException when save violates unique constraint")
    void register_ShouldThrowUserAlreadyExistsException_WhenSaveViolatesUniqueConstraint() {
        RegisterCommand command = RegisterCommand.builder()
            .email(EMAIL)
            .password(RAW_PASSWORD)
            .firstName(FIRST_NAME)
            .build();

        User user = User.builder()
            .email(EMAIL)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.empty());
        when(registerCommandMapper.toEntity(command)).thenReturn(user);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(user)).thenThrow(new DataIntegrityViolationException("duplicate email"));

        assertThatThrownBy(() -> registrationService.register(command))
            .isInstanceOf(UserAlreadyExistsException.class);

        verifyNoInteractions(userActivationService);
    }

    @Test
    @DisplayName("register should save user with encoded password and initialize activation when email is free")
    void register_ShouldSaveUserAndInitializeActivation_WhenEmailIsFree() {
        RegisterCommand command = RegisterCommand.builder()
            .email(EMAIL)
            .password(RAW_PASSWORD)
            .firstName(FIRST_NAME)
            .build();

        User user = User.builder()
            .email(EMAIL)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.empty());
        when(registerCommandMapper.toEntity(command)).thenReturn(user);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        registrationService.register(command);

        assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);

        verify(userRepository).save(user);
        verify(userActivationService).initializeActivation(EMAIL, FIRST_NAME);
    }

    @Test
    @DisplayName("provisionOAuthUser should return existing user when user is active")
    void provisionOAuthUser_ShouldReturnExistingUser_WhenUserIsActive() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .build();

        User existingUser = User.builder()
            .email(EMAIL)
            .userStatus(UserStatus.ACTIVE)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

        User result = registrationService.provisionOAuthUser(command);

        assertThat(result).isSameAs(existingUser);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(oauthProvisionCommandMapper, passwordEncoder, randomPasswordGenerator);
    }

    @Test
    @DisplayName("provisionOAuthUser should activate pending user and remove activation token")
    void provisionOAuthUser_ShouldActivatePendingUser_WhenUserIsPending() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .build();

        UserActivationToken activationToken = UserActivationToken.builder()
            .token("activation-token")
            .build();

        User pendingUser = User.builder()
            .email(EMAIL)
            .userStatus(UserStatus.PENDING)
            .userActivationToken(activationToken)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(pendingUser));
        when(userRepository.save(pendingUser)).thenReturn(pendingUser);

        User result = registrationService.provisionOAuthUser(command);

        assertThat(result).isSameAs(pendingUser);
        assertThat(pendingUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(pendingUser.getUserActivationToken()).isNull();

        verify(userRepository).save(pendingUser);
        verifyNoInteractions(oauthProvisionCommandMapper, passwordEncoder, randomPasswordGenerator);
    }

    @Test
    @DisplayName("provisionOAuthUser should throw UserNotActivatedException when user is blocked")
    void provisionOAuthUser_ShouldThrowUserNotActivatedException_WhenUserIsBlocked() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .build();

        User blockedUser = User.builder()
            .email(EMAIL)
            .userStatus(UserStatus.BLOCKED)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.of(blockedUser));

        assertThatThrownBy(() -> registrationService.provisionOAuthUser(command))
            .isInstanceOf(UserNotActivatedException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(oauthProvisionCommandMapper, passwordEncoder, randomPasswordGenerator);
    }

    @Test
    @DisplayName("provisionOAuthUser should create user with encoded random password when user does not exist")
    void provisionOAuthUser_ShouldCreateUserWithEncodedRandomPassword_WhenUserNotFound() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .build();

        User newUser = User.builder()
            .email(EMAIL)
            .build();

        User savedUser = User.builder()
            .id(1L)
            .email(EMAIL)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.empty());
        when(oauthProvisionCommandMapper.toEntity(command)).thenReturn(newUser);
        when(randomPasswordGenerator.generate()).thenReturn(RANDOM_PASSWORD);
        when(passwordEncoder.encode(RANDOM_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(newUser)).thenReturn(savedUser);

        User result = registrationService.provisionOAuthUser(command);

        assertThat(result).isSameAs(savedUser);
        assertThat(newUser.getPassword()).isEqualTo(ENCODED_PASSWORD);

        verify(userRepository).save(newUser);
        verifyNoInteractions(userActivationService);
    }

    @Test
    @DisplayName("provisionOAuthUser should resolve existing user when concurrent creation violates unique constraint")
    void provisionOAuthUser_ShouldResolveExistingUser_WhenConcurrentCreationDetected() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .build();

        User newUser = User.builder()
            .email(EMAIL)
            .build();

        User concurrentlyCreatedUser = User.builder()
            .email(EMAIL)
            .userStatus(UserStatus.ACTIVE)
            .build();

        when(userRepository.findUserByEmail(EMAIL))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(concurrentlyCreatedUser));
        when(oauthProvisionCommandMapper.toEntity(command)).thenReturn(newUser);
        when(randomPasswordGenerator.generate()).thenReturn(RANDOM_PASSWORD);
        when(passwordEncoder.encode(RANDOM_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(newUser)).thenThrow(new DataIntegrityViolationException("duplicate email"));

        User result = registrationService.provisionOAuthUser(command);

        assertThat(result).isSameAs(concurrentlyCreatedUser);

        verify(userRepository, times(2)).findUserByEmail(EMAIL);
    }

    @Test
    @DisplayName("provisionOAuthUser should throw IllegalStateException when user not found after constraint violation")
    void provisionOAuthUser_ShouldThrowIllegalStateException_WhenConstraintViolatedButUserNotFound() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .build();

        User newUser = User.builder()
            .email(EMAIL)
            .build();

        when(userRepository.findUserByEmail(EMAIL)).thenReturn(Optional.empty());
        when(oauthProvisionCommandMapper.toEntity(command)).thenReturn(newUser);
        when(randomPasswordGenerator.generate()).thenReturn(RANDOM_PASSWORD);
        when(passwordEncoder.encode(RANDOM_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(newUser)).thenThrow(new DataIntegrityViolationException("duplicate email"));

        assertThatThrownBy(() -> registrationService.provisionOAuthUser(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unique constraint violated but no row found")
            .hasCauseInstanceOf(DataIntegrityViolationException.class);

        verify(userRepository, times(2)).findUserByEmail(EMAIL);
    }
}