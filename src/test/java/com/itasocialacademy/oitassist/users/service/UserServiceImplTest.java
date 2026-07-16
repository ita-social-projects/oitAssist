package com.itasocialacademy.oitassist.users.service;

import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.*;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.service.UserServiceImpl;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    @DisplayName("changeUserRole should update user role when user is admin and request is valid")
    void changeUserRole_ShouldUpdatedUserRole_WhenUserIsAdminAndRequestIsValid() {
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

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(repository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);
        when(mapper.toResponseUserDTO(user)).thenReturn(expected);

        ResponseUserDTO result = userService.changeUserRole(targetUserId, Role.ORG);

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(Role.ORG);
        assertThat(user.getRole()).isEqualTo(Role.ORG);

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
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

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(repository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeUserRole(targetUserId, Role.ORG))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessage("User not found");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
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

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(repository.findById(targetUserId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.changeUserRole(targetUserId, Role.USER))
            .isInstanceOf(AdminRoleModificationException.class)
            .hasMessage("Cannot modify role of another administrator");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
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

    @Test
    @DisplayName("changeUserRole should throw InsufficientPermissionsException when user is not admin")
    void changeUserRole_ShouldThrowInsufficientPermissionsException_WhenUserIsNotAdmin() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(false);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));

        assertThatThrownBy(() -> userService.changeUserRole(targetUserId, Role.ORG))
            .isInstanceOf(InsufficientPermissionsException.class)
            .hasMessage("You do not have enough permissions to perform this action");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(securityFacade).getCurrentUserId();
        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("getUsers should return paged users when current user is admin")
    void getUsers_ShouldReturnPagedUsers_WhenCurrentUserIsAdmin() {
        Pageable pageable = PageRequest.of(0, 10);
        String search = "ivan";

        User user = User.builder()
            .id(1L)
            .email("ivan@example.com")
            .firstName("Ivan")
            .surname("Petrenko")
            .middleName("Ivanovych")
            .role(Role.USER)
            .userStatus(UserStatus.ACTIVE)
            .build();

        ResponseUserDTO responseUserDTO = ResponseUserDTO.builder()
            .id(1L)
            .email("ivan@example.com")
            .firstName("Ivan")
            .lastName("Petrenko")
            .middleName("Ivanovych")
            .role(Role.USER)
            .status(UserStatus.ACTIVE)
            .build();

        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(repository.findAllBySearch("ivan", pageable)).thenReturn(userPage);
        when(mapper.toResponseUserDTO(user)).thenReturn(responseUserDTO);

        Page<ResponseUserDTO> result = userService.getUsers(pageable, search);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);
        assertThat(result.getContent().getFirst().getEmail()).isEqualTo("ivan@example.com");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(repository).findAllBySearch("ivan", pageable);
        verify(mapper).toResponseUserDTO(user);
    }

    @Test
    @DisplayName("getUsers should throw InsufficientPermissionsException when current user is not admin")
    void getUsers_ShouldThrowInsufficientPermissionsException_WhenCurrentUserIsNotAdmin() {
        Pageable pageable = PageRequest.of(0, 10);

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(false);

        assertThatThrownBy(() -> userService.getUsers(pageable, "ivan"))
            .isInstanceOf(InsufficientPermissionsException.class)
            .hasMessage("You do not have enough permissions to perform this action");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("getUsers should normalize search query before repository call when search contains extra spaces")
    void getUsers_ShouldNormalizeSearchQuery_WhenSearchContainsExtraSpaces() {
        Pageable pageable = PageRequest.of(0, 10);
        String search = "  Ivan   Petrenko%  ";
        String normalizedSearch = "Ivan Petrenko\\%";

        Page<User> emptyPage = Page.empty(pageable);

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(repository.findAllBySearch(normalizedSearch, pageable)).thenReturn(emptyPage);

        Page<ResponseUserDTO> result = userService.getUsers(pageable, search);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(repository).findAllBySearch(normalizedSearch, pageable);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("getUsers should use findAll when search is null")
    void getUsers_ShouldUseEmptySearchString_WhenSearchIsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyPage = Page.empty(pageable);

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(repository.findAll(pageable)).thenReturn(emptyPage);

        Page<ResponseUserDTO> result = userService.getUsers(pageable, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(repository).findAll(pageable);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("changeUserStatus should update user status when user is admin and request is valid")
    void changeUserStatus_ShouldUpdatedUserStatus_WhenUserIsAdminAndRequestIsValid() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        User user = User.builder()
            .id(targetUserId)
            .userStatus(UserStatus.INACTIVE)
            .role(Role.USER)
            .build();

        ResponseUserDTO expected = ResponseUserDTO.builder()
            .id(targetUserId)
            .status(UserStatus.ACTIVE)
            .build();

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(repository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);
        when(mapper.toResponseUserDTO(user)).thenReturn(expected);

        ResponseUserDTO result = userService.changeUserStatus(targetUserId, UserStatus.ACTIVE);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(securityFacade).getCurrentUserId();
        verify(repository).findById(targetUserId);
        verify(repository).save(user);
        verify(mapper).toResponseUserDTO(user);
    }

    @Test
    @DisplayName("changeUserStatus should throw UserStatusSelfChangeException when user tries to change own status")
    void changeUserStatus_ShouldThrowUserStatusSelfChangeException_WhenChangingOwnStatus() {
        Long currentUserId = 1L;

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));

        assertThatThrownBy(() -> userService.changeUserStatus(currentUserId, UserStatus.ACTIVE))
            .isInstanceOf(UserStatusSelfChangeException.class)
            .hasMessage("User cannot change their own status");

        verify(securityFacade).getCurrentUserId();
        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("changeUserStatus should throw UserNotFoundException when target user does not exist")
    void changeUserStatus_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(repository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeUserStatus(targetUserId, UserStatus.ACTIVE))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessage("User not found");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(securityFacade).getCurrentUserId();
        verify(repository).findById(targetUserId);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("changeUserStatus should throw AdminStatusModificationException when target user is admin")
    void changeUserStatus_ShouldThrowAdminStatusModificationException_WhenTargetUserIsAdmin() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        User admin = User.builder()
            .id(targetUserId)
            .role(Role.ADMIN)
            .build();

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
        when(repository.findById(targetUserId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.changeUserStatus(targetUserId, UserStatus.BLOCKED))
            .isInstanceOf(AdminStatusModificationException.class)
            .hasMessage("Cannot modify status of another administrator");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(securityFacade).getCurrentUserId();
        verify(repository).findById(targetUserId);
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("changeUserStatus should throw AuthorizationException when user is not authenticated")
    void changeUserStatus_ShouldThrowAuthorizationException_WhenUserIsNotAuthenticated() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeUserStatus(1L, UserStatus.ACTIVE))
            .isInstanceOf(AuthorizationException.class)
            .hasMessage("User is not authenticated");

        verify(securityFacade).getCurrentUserId();
        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("changeUserStatus should throw InsufficientPermissionsException when user is not admin")
    void changeUserStatus_ShouldThrowInsufficientPermissionsException_WhenUserIsNotAdmin() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(false);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(currentUserId));

        assertThatThrownBy(() -> userService.changeUserStatus(targetUserId, UserStatus.ACTIVE))
            .isInstanceOf(InsufficientPermissionsException.class)
            .hasMessage("You do not have enough permissions to perform this action");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(securityFacade).getCurrentUserId();
        verifyNoInteractions(repository, mapper);
    }
}