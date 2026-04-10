package com.itasocialacademy.oitassist.auth.service;

import com.itasocialacademy.oitassist.auth.AuthTestDataFactory;
import com.itasocialacademy.oitassist.auth.dto.request.RegisterRequest;
import com.itasocialacademy.oitassist.auth.exceptions.UserNotActivatedException;
import com.itasocialacademy.oitassist.auth.mapper.RegisterRequestMapper;
import com.itasocialacademy.oitassist.auth.service.interfaces.UserActivationService;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for Registration Service")
class RegistrationServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RegisterRequestMapper registerRequestMapper;

    @Mock
    private UserActivationService userActivationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    @DisplayName("User already exists but not activated")
    void createUser_userAlreadyExistsNotActivated_shouldThrowUserNotActivatedException() {
        // given
        User user = mock(User.class);
        RegisterRequest request = AuthTestDataFactory.validRegisterRequest();

        // when
        when(user.getUserStatus()).thenReturn(UserStatus.NOT_ACTIVATED);
        when(userRepository.findUserByEmail(request.getEmail()))
            .thenReturn(Optional.of(user));

        // then
        assertThrows(UserNotActivatedException.class, () -> registrationService.createUser(request));
        verify(registerRequestMapper, never()).toEntity(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(userActivationService, never()).initializeActivation(any(), any());
    }

    @Test
    @DisplayName("User already exists and activated")
    void createUser_userAlreadyExistsActivated_shouldThrowUserAlreadyExistsException() {
        // given
        User user = mock(User.class);
        RegisterRequest request = AuthTestDataFactory.validRegisterRequest();

        // when
        when(user.getUserStatus()).thenReturn(UserStatus.ACTIVATED);
        when(userRepository.findUserByEmail(request.getEmail()))
            .thenReturn(Optional.of(user));

        // then
        assertThrows(UserAlreadyExistsException.class, () -> registrationService.createUser(request));
        verify(registerRequestMapper, never()).toEntity(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(userActivationService, never()).initializeActivation(any(), any());
    }

    @Test
    @DisplayName("User not found and new created")
    void createUser_userNotFound_newUserCreated() {
        // given
        RegisterRequest request = AuthTestDataFactory.validRegisterRequest();

        // when
        when(userRepository.findUserByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(registerRequestMapper.toEntity(request)).thenReturn(new User());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        registrationService.createUser(request);

        // then
        verify(userRepository, times(1)).findUserByEmail(request.getEmail());
        verify(registerRequestMapper, times(1)).toEntity(request);
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userActivationService).initializeActivation(request.getEmail(), request.getFirstName());
    }
}
