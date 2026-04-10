package com.itasocialacademy.oitassist.auth.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.auth.AuthTestDataFactory;
import com.itasocialacademy.oitassist.auth.dto.request.RegisterRequest;
import com.itasocialacademy.oitassist.auth.service.interfaces.RegistrationService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Unit tests for Registration Controller")
class RegistrationControllerTest extends ControllerUnitTest {
    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private RegistrationController controller;

    @Override
    protected Object getController() {
        return controller;
    }

    @Test
    @DisplayName("Request with valid data")
    void createUser_validData_shouldReturn201() throws Exception {
        // given
        RegisterRequest request = AuthTestDataFactory.validRegisterRequest();

        // when
        doNothing().when(registrationService).createUser(any(RegisterRequest.class));

        mockMvc.perform(post(AuthTestDataFactory.REGISTRATION_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // then
        verify(registrationService, times(1))
            .createUser(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Request with invalid data")
    void createUser_invalidRequest_shouldReturn400() throws Exception {
        // given
        RegisterRequest request = AuthTestDataFactory.invalidRegisterRequest();

        // when
        mockMvc.perform(post(AuthTestDataFactory.REGISTRATION_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.errors").isMap())
            .andExpect(jsonPath("$.details.errors.firstName").doesNotExist())
            .andExpect(jsonPath("$.details.errors.lastName").isString())
            .andExpect(jsonPath("$.details.errors.password").isString())
            .andExpect(jsonPath("$.details.errors.phoneNumber").isString())
            .andExpect(jsonPath("$.details.errors.middleName").isString())
            .andExpect(jsonPath("$.details.errors.email").isString())
            .andExpect(jsonPath("$.code").value(ErrorCode.COMMON_VALIDATION_FAILED.name()))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.path").value(AuthTestDataFactory.REGISTRATION_PATH));

        // then
        verify(registrationService, never()).createUser(any());
    }
}
