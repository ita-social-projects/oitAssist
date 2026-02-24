package com.itasocialacademy.oitassist.auth.controller;

import com.itasocialacademy.oitassist.auth.dao.dto.request.ResendVerificationMailRequest;
import com.itasocialacademy.oitassist.auth.exceptions.UserAlreadyActivatedException;
import com.itasocialacademy.oitassist.auth.service.interfaces.UserActivationService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper;
import com.itasocialacademy.oitassist.core.web.GlobalExceptionHandler;
import com.itasocialacademy.oitassist.user.exceptions.ActivationTokenSendingTimeoutException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserActivationControllerTest {
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AppExceptionHttpStatusMapper mapper = new AppExceptionHttpStatusMapper();

    @Mock
    private UserActivationService userActivationService;

    @InjectMocks
    private UserActivationController userActivationController;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(userActivationController)
            .setControllerAdvice(new GlobalExceptionHandler(mapper))
            .build();
    }

    @Test
    void resendVerificationEmail_userNotFound_shouldThrow404() throws Exception {
        // given
        String email = "test@test.com";
        String path = "/api/v1/user-activation/resend";
        ResendVerificationMailRequest request = ResendVerificationMailRequest
            .builder()
            .email(email)
            .build();

        doThrow(new UserNotFoundException()).when(userActivationService).resendVerificationEmail(email);

        // when + then
        mockMvc.perform(post("/api/v1/user-activation/resend")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.details.errors").doesNotExist())
            .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.name()))
            .andExpect(jsonPath("$.message").value("User not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.path").value(path));

    }

    @Test
    void resendVerificationEmail_userActivated_shouldThrow409() throws Exception {
        // given
        String email = "test@test.com";
        String path = "/api/v1/user-activation/resend";
        ResendVerificationMailRequest request = ResendVerificationMailRequest
            .builder()
            .email(email)
            .build();

        doThrow(new UserAlreadyActivatedException()).when(userActivationService).resendVerificationEmail(email);

        // when + then
        mockMvc.perform(post("/api/v1/user-activation/resend")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.details.errors").doesNotExist())
            .andExpect(jsonPath("$.code").value(ErrorCode.USER_ALREADY_ACTIVATED.name()))
            .andExpect(jsonPath("$.message").value("User already activated"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.path").value(path));
    }

    @Test
    void resendVerificationEmail_tokenNotFoundOrExpired_shouldReturn200() throws Exception {
        // given
        String email = "test@test.com";
        String path = "/api/v1/user-activation/resend";
        ResendVerificationMailRequest request = ResendVerificationMailRequest
            .builder()
            .email(email)
            .build();

        doNothing().when(userActivationService).resendVerificationEmail(email);

        // when + then
        mockMvc.perform(post("/api/v1/user-activation/resend")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void resendVerificationEmail_resendTimeoutNotExceeded_shouldThrow400() throws Exception {
        // given
        String email = "test@test.com";
        String path = "/api/v1/user-activation/resend";
        ResendVerificationMailRequest request = ResendVerificationMailRequest
            .builder()
            .email(email)
            .build();

        doThrow(new ActivationTokenSendingTimeoutException(Instant.now().plus(2, ChronoUnit.MINUTES)))
            .when(userActivationService).resendVerificationEmail(email);

        // when + then
        mockMvc.perform(post("/api/v1/user-activation/resend")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details").isMap())
            .andExpect(jsonPath("$.details.sendingAvailableAfter").isString())
            .andExpect(jsonPath("$.code").value(ErrorCode.ACTIVATION_EMAIL_SENDING_TIMEOUT.name()))
            .andExpect(jsonPath("$.message").value("Activation token sending timeout. Please try again later."))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.path").value(path));
    }
}