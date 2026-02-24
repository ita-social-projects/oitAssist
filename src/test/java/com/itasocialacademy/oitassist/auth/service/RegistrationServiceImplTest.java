package com.itasocialacademy.oitassist.auth.service;

import com.itasocialacademy.oitassist.auth.dao.dto.request.RegisterRequest;
import com.itasocialacademy.oitassist.auth.exceptions.UserNotActivatedException;
import com.itasocialacademy.oitassist.auth.mapper.RegisterRequestMapper;
import com.itasocialacademy.oitassist.auth.service.interfaces.UserActivationService;
import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RegisterRequestMapper registerRequestMapper;

    @Mock
    private UserActivationService userActivationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void createUser_userAlreadyExistsNotActivated_shouldThrowException() {
        // given
        String email = "test@mail.com";
        User user = mock(User.class);
        RegisterRequest request = mock(RegisterRequest.class);

        // when + then
        when(user.getUserStatus()).thenReturn(UserStatus.NOT_ACTIVATED);
        when(request.getEmail()).thenReturn(email);
        when(userRepository.findUserByEmail(email))
            .thenReturn(Optional.of(user));

        assertThrows(UserNotActivatedException.class, () -> registrationService.createUser(request));
        verify(emailService, never()).sendHtmlEmail(any(), any(), any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_userAlreadyExistsActivated_shouldThrowException() {
        // given
        String email = "test@mail.com";
        User user = mock(User.class);
        RegisterRequest request = mock(RegisterRequest.class);

        // when + then
        when(user.getUserStatus()).thenReturn(UserStatus.ACTIVATED);
        when(request.getEmail()).thenReturn(email);
        when(userRepository.findUserByEmail(email))
            .thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistsException.class, () -> registrationService.createUser(request));
        verify(emailService, never()).sendHtmlEmail(any(), any(), any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_userNotFound_newUserCreated() {
        // given
        String email = "test@mail.com";
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setFirstName("Test");
        request.setLastName("Test");
        request.setMiddleName("Test");
        request.setPhoneNumber("+380991234567");
        request.setPassword("password123");

        // when + then
        when(userRepository.findUserByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(registerRequestMapper.toEntity(request)).thenReturn(new User());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        doNothing().when(userActivationService).publishActivationEvent(eq(request.getEmail()),
            eq(request.getFirstName()),
            anyString());

        registrationService.createUser(request);

        verify(userRepository, times(1)).save(any(User.class));
        verify(registerRequestMapper, times(1)).toEntity(eq(request));
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(userActivationService, times(1)).publishActivationEvent(eq(request.getEmail()),
            eq(request.getFirstName()),
            anyString());
    }
}
