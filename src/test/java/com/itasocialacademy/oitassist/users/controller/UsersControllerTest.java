package com.itasocialacademy.oitassist.users.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.user.controller.UserController;
import com.itasocialacademy.oitassist.user.dao.dto.request.ChangeUserRoleRequest;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
            .andExpect(jsonPath("$.firstName").value("Bob"))
            .andExpect(jsonPath("$.lastName").value("Smith"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService, times(1)).getCurrentUserProfile();
    }

    @Test
    @DisplayName("changeUserRole should return updated user when request is valid")
    void changeUserRole_ShouldReturnUpdatedUser_WhenRequestIsValid() throws Exception {
        Long userId = 1L;

        ChangeUserRoleRequest request = ChangeUserRoleRequest.builder()
            .role(Role.ORG)
            .build();

        ResponseUserDTO response = ResponseUserDTO.builder()
            .id(userId)
            .role(Role.ORG)
            .build();

        when(userService.changeUserRole(userId, Role.ORG))
            .thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/{id}/role", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.role").value("ORG"));

        verify(userService).changeUserRole(userId, Role.ORG);
    }

    @Test
    @DisplayName("changeUserRole should return bad request when request body is invalid")
    void changeUserRole_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {

        mockMvc.perform(patch("/api/v1/users/{id}/role", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }
}