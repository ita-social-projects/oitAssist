package com.itasocialacademy.oitassist.users.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.user.controller.UserController;
import com.itasocialacademy.oitassist.user.dao.dto.request.ChangeUserRoleRequest;
import com.itasocialacademy.oitassist.user.dao.dto.request.ChangeUserStatusRequest;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Test
    @DisplayName("getUsers should return paged users when request is valid")
    void getUsers_ShouldReturnPagedUsers_WhenRequestIsValid() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);

        ResponseUserDTO user = ResponseUserDTO.builder()
            .id(1L)
            .email("ivan@example.com")
            .firstName("Ivan")
            .lastName("Petrenko")
            .middleName("Ivanovych")
            .role(Role.USER)
            .status(UserStatus.ACTIVE)
            .build();

        Page<ResponseUserDTO> page = new PageImpl<>(List.of(user), pageable, 1);

        when(userService.getUsers(any(Pageable.class), eq("ivan"))).thenReturn(page);

        mockMvc.perform(get("/api/v1/users")
            .param("page", "0")
            .param("size", "10")
            .param("search", "ivan"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].email").value("ivan@example.com"))
            .andExpect(jsonPath("$.content[0].firstName").value("Ivan"))
            .andExpect(jsonPath("$.content[0].lastName").value("Petrenko"))
            .andExpect(jsonPath("$.content[0].middleName").value("Ivanovych"))
            .andExpect(jsonPath("$.content[0].role").value("USER"))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));

        verify(userService).getUsers(any(Pageable.class), eq("ivan"));
    }

    @Test
    @DisplayName("getUsers should return paged users when search parameter is not provided")
    void getUsers_ShouldReturnPagedUsers_WhenSearchParameterIsNotProvided() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ResponseUserDTO> emptyPage = Page.empty(pageable);

        when(userService.getUsers(any(Pageable.class), isNull())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/users")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));

        verify(userService).getUsers(any(Pageable.class), isNull());
    }

    @Test
    @DisplayName("changeUserStatus should return updated user when request is valid")
    void changeUserStatus_ShouldReturnUpdatedUser_WhenRequestIsValid() throws Exception {
        Long userId = 1L;

        ChangeUserStatusRequest request = ChangeUserStatusRequest.builder()
            .status(UserStatus.ACTIVE)
            .build();

        ResponseUserDTO response = ResponseUserDTO.builder()
            .id(userId)
            .status(UserStatus.ACTIVE)
            .build();

        when(userService.changeUserStatus(userId, UserStatus.ACTIVE))
            .thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/{id}/status", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).changeUserStatus(userId, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("changeUserStatus should return bad request when request body is invalid")
    void changeUserStatus_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {

        mockMvc.perform(patch("/api/v1/users/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }
}