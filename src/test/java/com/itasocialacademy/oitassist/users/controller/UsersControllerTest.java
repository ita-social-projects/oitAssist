package com.itasocialacademy.oitassist.users.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.user.controller.UserController;
import com.itasocialacademy.oitassist.user.dao.dto.request.ProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.exceptions.ProfileUpdateRequestException;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Unit tests for Users Controller")
class UsersControllerTest extends ControllerUnitTest<UserController> {
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Override
    protected UserController getController() {
        return userController;
    }

    @Test
    @DisplayName("GET /profile should return 200 with user profile when authenticated")
    void getProfile_ShouldReturnOkWithBody_IfUserIsAuthenticated() throws Exception {
        // given
        ResponseUserDTO profile = ResponseUserDTO.builder()
            .id(1L)
            .email("test@email.com")
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .role(Role.USER)
            .status(UserStatus.ACTIVE)
            .build();

        // when
        when(userService.getCurrentUserProfile()).thenReturn(profile);

        mockMvc.perform(
            get("/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.email").value("test@email.com"))
            .andExpect(jsonPath("$.middleName").value("John"))
            .andExpect(jsonPath("$.phoneNumber").value("380931111111"))
            .andExpect(jsonPath("$.firstName").value("Bob"))
            .andExpect(jsonPath("$.lastName").value("Smith"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService, times(1)).getCurrentUserProfile();
    }

    @Test
    @DisplayName("Post /profile/update-request should return 201 when authenticated")
    void createProfileUpdateRequest_ShouldReturnCreated_whenRequestIsValid() throws Exception {
        // given
        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        // when
        doNothing().when(userService).createProfileUpdateRequest(request);

        mockMvc.perform(
            post("/api/v1/users/profile/update-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // then
            .andExpect(status().isCreated());

        verify(userService, times(1)).createProfileUpdateRequest(request);
    }

    @Test
    @DisplayName("Post /profile/update-request should throw ProfileUpdateRequestException when some request has status pending")
    void createProfileUpdateRequest_shouldThrow_whenPendingExists() throws Exception {
        // given
        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        // when
        doThrow(new ProfileUpdateRequestException("User already have a pending update request",
            ErrorCode.PROFILE_UPDATE_REQUEST_ALREADY_PENDING))
            .when(userService).createProfileUpdateRequest(request);

        mockMvc.perform(
            post("/api/v1/users/profile/update-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // then
            .andExpect(status().isConflict());

        verify(userService, times(1)).createProfileUpdateRequest(request);
    }

    @Test
    @DisplayName("Post /profile/update-request should throw ProfileUpdateRequestException when user already had an update request during the day")
    void createProfileUpdateRequest_shouldThrow_whenAlreadyHadARequestToday() throws Exception {
        // given
        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        // when
        doThrow(new ProfileUpdateRequestException("User already had a request today",
            ErrorCode.PROFILE_UPDATE_REQUEST_DAILY_LIMIT))
            .when(userService).createProfileUpdateRequest(request);

        mockMvc.perform(
            post("/api/v1/users/profile/update-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // then
            .andExpect(status().isConflict());

        verify(userService, times(1)).createProfileUpdateRequest(request);
    }

    @Test
    @DisplayName("Post /profile/update-request should return 400 when body is invalid")
    void createProfileUpdateRequest_shouldReturn400_whenBodyIsIncorrect() throws Exception {
        // given
        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        // when
        mockMvc.perform(
            post("/api/v1/users/profile/update-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // then
            .andExpect(status().isBadRequest());

        verify(userService, never()).createProfileUpdateRequest(any());
    }

}