package com.itasocialacademy.oitassist.users.service;

import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.AdminRoleModificationException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import com.itasocialacademy.oitassist.user.exceptions.UserRoleSelfChangeException;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.service.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit test for UserServiceImpl")
class UserServiceImplTest {
    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    @Mock
    private SecurityFacade securityFacade;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("loadUserByEmail should return DTO when user exists")
    void loadUserByEmail_ShouldReturnResponseUserDto_IfUserExists() {
        String email = "test@email.com";

        User user = User.builder()
            .email(email)
            .build();

        ResponseUserDTO expected = ResponseUserDTO.builder()
            .email(email)
            .build();

        when(repository.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(mapper.toResponseUserDTO(user)).thenReturn(expected);

        ResponseUserDTO result = userService.loadUserByEmail(email);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);

        verify(repository, times(1)).findUserByEmail(email);
        verify(mapper, times(1)).toResponseUserDTO(user);
    }

    @Test
    @DisplayName("loadUserByEmail should throw EntityNotFoundException when user not found")
    void loadUserByEmail_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        String email = "test@email.com";

        when(repository.findUserByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByEmail(email))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("User not found");

        verify(repository, times(1)).findUserByEmail(email);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("getCurrentUserProfile should return profile when user is authenticated")
    void getCurrentUserProfile_ShouldReturnResponseUserDto_WhenUserIsAuthenticated() {
        String email = "test@email.com";

        User user = User.builder()
            .email(email)
            .build();

        ResponseUserDTO expected = ResponseUserDTO.builder()
            .id(1L)
            .email(email)
            .firstName("Bob")
            .lastName("Smith")
            .role(Role.USER)
            .status(UserStatus.ACTIVE)
            .build();

        when(securityFacade.getCurrentUserEmail()).thenReturn(Optional.of(email));
        when(repository.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(mapper.toResponseUserDTO(user)).thenReturn(expected);

        ResponseUserDTO result = userService.getCurrentUserProfile();

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.getFirstName()).isEqualTo("Bob");
        assertThat(result.getLastName()).isEqualTo("Smith");

        verify(securityFacade, times(1)).getCurrentUserEmail();
        verify(repository, times(1)).findUserByEmail(email);
        verify(mapper, times(1)).toResponseUserDTO(user);
    }

    @Test
    @DisplayName("getCurrentUserProfile should throw AuthorizationException when user is not authenticated")
    void getCurrentUserProfile_ShouldThrowAuthorizationException_WhenUserIsNotAuthenticated() {
        when(securityFacade.getCurrentUserEmail()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUserProfile())
            .isInstanceOf(AuthorizationException.class)
            .hasMessage("User is not authenticated");

        verify(securityFacade, times(1)).getCurrentUserEmail();
        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("changeUserRole should update user role when request is valid")
    void changeUserRole_ShouldUpdateUserRole_WhenRequestIsValid() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        User user = User.builder()
            .id(targetUserId)
            .role(Role.USER)
            .build();

        ResponseUserDTO expected = ResponseUserDTO.builder()
            .id(targetUserId)
            .role(Role.ORG)
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(repository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);
        when(mapper.toResponseUserDTO(user)).thenReturn(expected);

        ResponseUserDTO result = userService.changeUserRole(targetUserId, Role.ORG);

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(Role.ORG);
        assertThat(user.getRole()).isEqualTo(Role.ORG);

        verify(securityFacade).getCurrentUserId();
        verify(repository).findById(targetUserId);
        verify(repository).save(user);
        verify(mapper).toResponseUserDTO(user);
    }

    @Test
    @DisplayName("changeUserRole should throw UserRoleSelfChangeException when user tries to change own role")
    void changeUserRole_ShouldThrowUserRoleSelfChangeException_WhenChangingOwnRole() {
        Long currentUserId = 1L;

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));

        assertThatThrownBy(() -> userService.changeUserRole(currentUserId, Role.USER))
            .isInstanceOf(UserRoleSelfChangeException.class)
            .hasMessage("User cannot change their own role");

        verify(securityFacade).getCurrentUserId();
        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("changeUserRole should throw UserNotFoundException when target user does not exist")
    void changeUserRole_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(repository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeUserRole(targetUserId, Role.ORG))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessage("User not found");

        verify(securityFacade).getCurrentUserId();
        verify(repository).findById(targetUserId);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("changeUserRole should throw AdminRoleModificationException when target user is admin")
    void changeUserRole_ShouldThrowAdminRoleModificationException_WhenTargetUserIsAdmin() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        User admin = User.builder()
            .id(targetUserId)
            .role(Role.ADMIN)
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(repository.findById(targetUserId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.changeUserRole(targetUserId, Role.USER))
            .isInstanceOf(AdminRoleModificationException.class)
            .hasMessage("Cannot modify role of another administrator");

        verify(securityFacade).getCurrentUserId();
        verify(repository).findById(targetUserId);
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("changeUserRole should throw AuthorizationException when user is not authenticated")
    void changeUserRole_ShouldThrowAuthorizationException_WhenUserIsNotAuthenticated() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeUserRole(1L, Role.USER))
            .isInstanceOf(AuthorizationException.class)
            .hasMessage("User is not authenticated");

        verify(securityFacade).getCurrentUserId();
        verifyNoInteractions(repository, mapper);
    }
}