package com.itasocialacademy.oitassist.auth.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.auth.AuthTestDataFactory;
import com.itasocialacademy.oitassist.auth.dto.request.ResendVerificationMailRequest;
import com.itasocialacademy.oitassist.auth.exceptions.UserAlreadyActivatedException;
import com.itasocialacademy.oitassist.auth.service.interfaces.UserActivationService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.user.exceptions.ActivationTokenSendingTimeoutException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Unit tests for User Activation Controller")
class UserActivationControllerTest extends ControllerUnitTest {
    @Mock
    private UserActivationService userActivationService;

    @InjectMocks
    private UserActivationController userActivationController;

    @Override
    protected Object getController() {
        return userActivationController;
    }

    @Test
    @DisplayName("Request with invalid data")
    void resendVerificationEmail_invalidRequest_shouldReturn400() throws Exception {
        // given
        ResendVerificationMailRequest request = AuthTestDataFactory.invalidResendVerificationMailRequest();

        // when
        mockMvc.perform(post(AuthTestDataFactory.ACTIVATION_RESEND_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.errors").isMap())
            .andExpect(jsonPath("$.details.errors.email").isString())
            .andExpect(jsonPath("$.code").value(ErrorCode.COMMON_VALIDATION_FAILED.name()))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.path").value(AuthTestDataFactory.ACTIVATION_RESEND_PATH));

        // then
        verify(userActivationService, never()).resendVerificationEmail(any());
    }

    @Test
    @DisplayName("Request with valid data but user not found")
    void resendVerificationEmail_userNotFound_shouldReturn404() throws Exception {
        // given
        ResendVerificationMailRequest request = AuthTestDataFactory.validResendVerificationMailRequest();

        // when
        doThrow(new UserNotFoundException()).when(userActivationService).resendVerificationEmail(request.getEmail());

        mockMvc.perform(post(AuthTestDataFactory.ACTIVATION_RESEND_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.details.errors").doesNotExist())
            .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.name()))
            .andExpect(jsonPath("$.message").value("User not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.path").value(AuthTestDataFactory.ACTIVATION_RESEND_PATH));

        // then
        verify(userActivationService, times(1)).resendVerificationEmail(request.getEmail());
    }

    @Test
    @DisplayName("Request with valid data but user already activated")
    void resendVerificationEmail_userActivated_shouldReturn409() throws Exception {
        // given
        ResendVerificationMailRequest request = AuthTestDataFactory.validResendVerificationMailRequest();

        // when
        doThrow(new UserAlreadyActivatedException()).when(userActivationService)
            .resendVerificationEmail(request.getEmail());

        mockMvc.perform(post(AuthTestDataFactory.ACTIVATION_RESEND_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.details.errors").doesNotExist())
            .andExpect(jsonPath("$.code").value(ErrorCode.USER_ALREADY_ACTIVATED.name()))
            .andExpect(jsonPath("$.message").value("User already activated"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.path").value(AuthTestDataFactory.ACTIVATION_RESEND_PATH));

        // then
        verify(userActivationService, times(1)).resendVerificationEmail(request.getEmail());
    }

    @Test
    @DisplayName("Request with valid data")
    void resendVerificationEmail_tokenNotFoundOrExpired_shouldReturn200() throws Exception {
        // given
        ResendVerificationMailRequest request = AuthTestDataFactory.validResendVerificationMailRequest();

        // when
        doNothing().when(userActivationService).resendVerificationEmail(request.getEmail());

        mockMvc.perform(post(AuthTestDataFactory.ACTIVATION_RESEND_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        // then
        verify(userActivationService, times(1)).resendVerificationEmail(request.getEmail());
    }

    @Test
    @DisplayName("Request with valid data but resend activation timeout is not exceeded")
    void resendVerificationEmail_resendTimeoutNotExceeded_shouldReturn400() throws Exception {
        // given
        ResendVerificationMailRequest request = AuthTestDataFactory.validResendVerificationMailRequest();

        // when
        doThrow(new ActivationTokenSendingTimeoutException(Instant.now().plus(2, ChronoUnit.MINUTES)))
            .when(userActivationService).resendVerificationEmail(request.getEmail());

        mockMvc.perform(post(AuthTestDataFactory.ACTIVATION_RESEND_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details").isMap())
            .andExpect(jsonPath("$.details.sendingAvailableAfter").isString())
            .andExpect(jsonPath("$.code").value(ErrorCode.ACTIVATION_EMAIL_SENDING_TIMEOUT.name()))
            .andExpect(jsonPath("$.message").value("Activation token sending timeout. Please try again later."))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.path").value(AuthTestDataFactory.ACTIVATION_RESEND_PATH));

        // then
        verify(userActivationService, times(1)).resendVerificationEmail(request.getEmail());
    }
}