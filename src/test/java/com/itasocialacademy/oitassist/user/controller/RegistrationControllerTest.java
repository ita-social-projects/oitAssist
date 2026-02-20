package com.itasocialacademy.oitassist.user.controller;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper;
import com.itasocialacademy.oitassist.core.web.GlobalExceptionHandler;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserRequest;
import com.itasocialacademy.oitassist.user.service.interfaces.RegistrationService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AppExceptionHttpStatusMapper mapper = new AppExceptionHttpStatusMapper();

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private RegistrationController registrationController;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(registrationController)
            .setControllerAdvice(new GlobalExceptionHandler(mapper))
            .build();
    }

    @Test
    void createUser_shouldReturn204_andCallService() throws Exception {
        // given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@test.com");
        request.setFirstName("Test");
        request.setLastName("Test");
        request.setMiddleName("Test");
        request.setPhoneNumber("+380991234567");
        request.setPassword("password123");

        doNothing().when(registrationService).createUser(any(CreateUserRequest.class));

        // when + then
        mockMvc.perform(post("/api/v1/registration")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(registrationService, times(1))
            .createUser(any(CreateUserRequest.class));
    }

    @Test
    void createUser_shouldReturn400_whenInvalidRequest() throws Exception {
        // given
        CreateUserRequest request = new CreateUserRequest();
        String path = "/api/v1/registration";

        // when + then
        mockMvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.errors").isMap())
            .andExpect(jsonPath("$.details.errors.firstName").isString())
            .andExpect(jsonPath("$.details.errors.lastName").isString())
            .andExpect(jsonPath("$.details.errors.password").isString())
            .andExpect(jsonPath("$.details.errors.phoneNumber").isString())
            .andExpect(jsonPath("$.details.errors.middleName").isString())
            .andExpect(jsonPath("$.details.errors.email").isString())
            .andExpect(jsonPath("$.code").value(ErrorCode.COMMON_VALIDATION_FAILED.name()))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.path").value(path));

        verify(registrationService, never())
            .createUser(any());
    }
}
