package com.itasocialacademy.oitassist.users.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.dao.dto.request.ProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.ProfileUpdateRequestRepository;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.ProfileUpdateRequestException;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.service.UserServiceImpl;
import com.itasocialacademy.oitassist.usercompetition.api.interfaces.UserCompetitionFacade;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
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
    private ProfileUpdateRequestRepository profileUpdateRequestRepository;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private UserCompetitionFacade userCompetitionFacade;

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
    void loadUserByEmail_ShouldThrowEntityNotFoundException_WhenUserNotFound() {
        String email = "test@email.com";

        when(repository.findUserByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByEmail(email))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("User not found: " + email);

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
    @DisplayName("Should create request with status PENDING when user has active competitions")
    void createProfileUpdateRequest_ShouldCreateRequestWithStatusPending_WhenUserHasActiveCompetitions() {
        // given
        String email = "test@email.com";

        User user = User.builder()
            .id(1L)
            .email(email)
            .build();

        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(user.getId()));
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(profileUpdateRequestRepository.existsByUserIdAndStatus(user.getId(), UpdateRequestStatus.PENDING))
            .thenReturn(false);
        when(profileUpdateRequestRepository.existsByUserIdAndRequestedAtBetween(eq(user.getId()), any(), any()))
            .thenReturn(false);
        when(userCompetitionFacade.hasActiveCompetitions(user.getId(),
            List.of(CompetitionStatus.INCOMING, CompetitionStatus.INPROGRESS))).thenReturn(true);

        // when
        userService.createProfileUpdateRequest(request);

        // then
        verify(profileUpdateRequestRepository).save(argThat(req -> req.getStatus() == UpdateRequestStatus.PENDING));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should create request with status Approved when user has active competitions")
    void createProfileUpdateRequest_ShouldCreateRequestWithStatusApproved_WhenUserDontHaveActiveCompetitions() {
        // given
        String email = "test@email.com";

        User user = User.builder()
            .id(1L)
            .email(email)
            .build();

        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(user.getId()));
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(profileUpdateRequestRepository.existsByUserIdAndStatus(user.getId(), UpdateRequestStatus.PENDING))
            .thenReturn(false);
        when(profileUpdateRequestRepository.existsByUserIdAndRequestedAtBetween(eq(user.getId()), any(), any()))
            .thenReturn(false);
        when(userCompetitionFacade.hasActiveCompetitions(user.getId(),
            List.of(CompetitionStatus.INCOMING, CompetitionStatus.INPROGRESS))).thenReturn(false);

        // when
        userService.createProfileUpdateRequest(request);

        // then
        verify(profileUpdateRequestRepository).save(argThat(req -> req.getStatus() == UpdateRequestStatus.APPROVED));
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw ProfileUpdateRequestException when user already had a request today")
    void createProfileUpdateRequest_ShouldThrow409_WhenUserAlreadyHadRequestToday() {
        // given
        String email = "test@email.com";

        User user = User.builder()
            .id(1L)
            .email(email)
            .build();

        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(user.getId()));
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(profileUpdateRequestRepository.existsByUserIdAndRequestedAtBetween(eq(user.getId()), any(), any()))
            .thenReturn(true);

        // when
        assertThatThrownBy(() -> userService.createProfileUpdateRequest(request))
            .isInstanceOf(ProfileUpdateRequestException.class)
            .hasMessage("User already had a request today");

        // then
        verify(profileUpdateRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ProfileUpdateRequestException when user already has a pending request")
    void createProfileUpdateRequest_ShouldThrow409_WhenUserAlreadyHasPendingRequest() {
        // given
        String email = "test@email.com";

        User user = User.builder()
            .id(1L)
            .email(email)
            .build();

        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(user.getId()));
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(profileUpdateRequestRepository.existsByUserIdAndStatus(user.getId(), UpdateRequestStatus.PENDING))
            .thenReturn(true);
        when(profileUpdateRequestRepository.existsByUserIdAndRequestedAtBetween(eq(user.getId()), any(), any()))
            .thenReturn(false);

        // when
        assertThatThrownBy(() -> userService.createProfileUpdateRequest(request))
            .isInstanceOf(ProfileUpdateRequestException.class)
            .hasMessage("User already have a pending update request");

        // then
        verify(profileUpdateRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AuthorizationException when user is not authenticated")
    void createProfileUpdateRequest_ShouldThrow401_WhenUserIsNotAuthenticated() {
        // given
        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> userService.createProfileUpdateRequest(request))
            .isInstanceOf(AuthorizationException.class);
    }
}