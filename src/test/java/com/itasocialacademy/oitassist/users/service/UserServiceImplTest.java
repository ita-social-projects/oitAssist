package com.itasocialacademy.oitassist.users.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.dao.dto.request.ProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.ReviewRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.ProfileUpdateRequest;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.ProfileUpdateRequestRepository;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.InvalidSortFieldException;
import com.itasocialacademy.oitassist.user.exceptions.ProfileUpdateRequestException;
import com.itasocialacademy.oitassist.user.mapper.ProfileUpdateRequestMapper;
import com.itasocialacademy.oitassist.user.exceptions.*;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.service.UserServiceImpl;
import com.itasocialacademy.oitassist.usercompetition.api.interfaces.UserCompetitionFacade;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.*;
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

    @Mock
    private ProfileUpdateRequestMapper profileUpdateRequestMapper;

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
    @DisplayName("getCurrentUserProfile should throw UserAuthorizationException when user is not authenticated")
    void getCurrentUserProfile_ShouldThrowUserAuthorizationException_WhenUserIsNotAuthenticated() {
        when(securityFacade.getCurrentUserEmail()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUserProfile())
            .isInstanceOf(UserAuthorizationException.class)
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
            List.of(CompetitionStatus.PUBLISHED, CompetitionStatus.ENROLLMENT))).thenReturn(true);

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
            List.of(CompetitionStatus.PUBLISHED, CompetitionStatus.ENROLLMENT))).thenReturn(false);

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

    @Test
    @DisplayName("Should return page when sort field is valid (requestedAt)")
    void getProfileUpdateRequests_shouldReturnPage_whenSortByRequestedAt() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("requestedAt"));

        Page<ProfileUpdateRequest> repoPage = new PageImpl<>(List.of());
        when(profileUpdateRequestRepository.findByStatus(UpdateRequestStatus.PENDING, pageable)).thenReturn(repoPage);

        assertThatNoException()
            .isThrownBy(() -> userService.getProfileUpdateRequests(UpdateRequestStatus.PENDING, pageable));
    }

    @Test
    @DisplayName("Should return page when sort field is valid (status)")
    void getProfileUpdateRequests_shouldReturnPage_whenSortByStatus() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("status"));

        Page<ProfileUpdateRequest> repoPage = new PageImpl<>(List.of());
        when(profileUpdateRequestRepository.findByStatus(UpdateRequestStatus.PENDING, pageable)).thenReturn(repoPage);

        assertThatNoException()
            .isThrownBy(() -> userService.getProfileUpdateRequests(UpdateRequestStatus.PENDING, pageable));
    }

    @Test
    @DisplayName("getProfileUpdateRequests - should call findAll when status is null")
    void getProfileUpdateRequests_shouldCallFindAll_whenStatusIsNull() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("requestedAt"));
        when(profileUpdateRequestRepository.findAll(pageable)).thenReturn(Page.empty());

        userService.getProfileUpdateRequests(null, pageable);

        verify(profileUpdateRequestRepository).findAll(pageable);
        verify(profileUpdateRequestRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("getProfileUpdateRequests - should throw InvalidSortFieldException when sort field is invalid")
    void getProfileUpdateRequests_shouldThrow_whenSortFieldIsInvalid() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("invalidField"));

        assertThatThrownBy(() -> userService.getProfileUpdateRequests(UpdateRequestStatus.PENDING, pageable))
            .isInstanceOf(InvalidSortFieldException.class)
            .hasMessageContaining("invalidField");
    }

    @Test
    @DisplayName("reviewProfileUpdateRequests - should approve request successfully")
    void reviewProfileUpdateRequests_shouldApprove_whenRequestIsPending() {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.APPROVED, null);

        User user = User.builder().id(10L).build();
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
            .id(id)
            .status(UpdateRequestStatus.PENDING)
            .user(user)
            .build();

        when(profileUpdateRequestRepository.findById(id)).thenReturn(Optional.of(request));
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));

        // when
        userService.reviewProfileUpdateRequests(id, body);

        // then
        verify(profileUpdateRequestRepository).save(request);
        assertThat(request.getStatus()).isEqualTo(UpdateRequestStatus.APPROVED);
    }

    @Test
    @DisplayName("reviewProfileUpdateRequests - should reject request with reason successfully")
    void reviewProfileUpdateRequests_shouldReject_whenReasonProvided() {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.REJECTED, "Wrong data");

        User user = User.builder().id(10L).build();
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
            .id(id)
            .status(UpdateRequestStatus.PENDING)
            .user(user)
            .build();

        when(profileUpdateRequestRepository.findById(id)).thenReturn(Optional.of(request));
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));

        // when
        userService.reviewProfileUpdateRequests(id, body);

        // then
        verify(profileUpdateRequestRepository).save(request);
        assertThat(request.getStatus()).isEqualTo(UpdateRequestStatus.REJECTED);
        assertThat(request.getRejectReason()).isEqualTo("Wrong data");
    }

    @Test
    @DisplayName("reviewProfileUpdateRequests - should throw when reject reason is blank")
    void reviewProfileUpdateRequests_shouldThrow_whenRejectReasonIsBlank() {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.REJECTED, "");

        // when & then
        assertThatThrownBy(() -> userService.reviewProfileUpdateRequests(id, body))
            .isInstanceOf(ProfileUpdateRequestException.class)
            .hasMessageContaining("Rejection reason cannot be blank");

        verifyNoInteractions(profileUpdateRequestRepository);
    }

    @Test
    @DisplayName("reviewProfileUpdateRequests - should throw when reject reason is null")
    void reviewProfileUpdateRequests_shouldThrow_whenRejectReasonIsNull() {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.REJECTED, null);

        // when & then
        assertThatThrownBy(() -> userService.reviewProfileUpdateRequests(id, body))
            .isInstanceOf(ProfileUpdateRequestException.class)
            .hasMessageContaining("Rejection reason cannot be blank");

        verifyNoInteractions(profileUpdateRequestRepository);
    }

    @Test
    @DisplayName("reviewProfileUpdateRequests - should throw EntityNotFoundException when request not found")
    void reviewProfileUpdateRequests_shouldThrow_whenRequestNotFound() {
        // given
        Long id = 999L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.APPROVED, null);

        when(profileUpdateRequestRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.reviewProfileUpdateRequests(id, body))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Request not found: " + id);
    }

    @Test
    @DisplayName("reviewProfileUpdateRequests - should throw ProfileUpdateRequestException when request already reviewed")
    void reviewProfileUpdateRequests_shouldThrow_whenRequestAlreadyReviewed() {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.APPROVED, null);

        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
            .id(id)
            .status(UpdateRequestStatus.APPROVED)
            .build();

        when(profileUpdateRequestRepository.findById(id)).thenReturn(Optional.of(request));

        // when & then
        assertThatThrownBy(() -> userService.reviewProfileUpdateRequests(id, body))
            .isInstanceOf(ProfileUpdateRequestException.class)
            .hasMessageContaining("Request is already reviewed");
    }

    @Test
    @DisplayName("reviewProfileUpdateRequests - should throw EntityNotFoundException when user not found")
    void reviewProfileUpdateRequests_shouldThrow_whenUserNotFound() {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.APPROVED, null);

        User user = User.builder().id(10L).build();
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
            .id(id)
            .status(UpdateRequestStatus.PENDING)
            .user(user)
            .build();

        when(profileUpdateRequestRepository.findById(id)).thenReturn(Optional.of(request));
        when(repository.findById(user.getId())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.reviewProfileUpdateRequests(id, body))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("User not found");
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
    @DisplayName("changeUserRole should throw UserAuthorizationException when user is not authenticated")
    void changeUserRole_ShouldThrowUserAuthorizationException_WhenUserIsNotAuthenticated() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeUserRole(1L, Role.USER))
            .isInstanceOf(UserAuthorizationException.class)
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
        when(repository.findAllBySearchAndRoles("ivan", null, pageable)).thenReturn(userPage);
        when(mapper.toResponseUserDTO(user)).thenReturn(responseUserDTO);

        Page<ResponseUserDTO> result = userService.getUsers(pageable, search, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);
        assertThat(result.getContent().getFirst().getEmail()).isEqualTo("ivan@example.com");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(repository).findAllBySearchAndRoles("ivan", null, pageable);
        verify(mapper).toResponseUserDTO(user);
    }

    @Test
    @DisplayName("getUsers should throw InsufficientPermissionsException when current user is not admin")
    void getUsers_ShouldThrowInsufficientPermissionsException_WhenCurrentUserIsNotAdmin() {
        Pageable pageable = PageRequest.of(0, 10);

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(false);

        assertThatThrownBy(() -> userService.getUsers(pageable, "ivan", List.of(Role.ADMIN)))
            .isInstanceOf(InsufficientPermissionsException.class)
            .hasMessage("You do not have enough permissions to perform this action");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("getUsers should normalize query parameters before repository call when search contains extra spaces or roles are empty")
    void getUsers_ShouldNormalizeQueryParameters_WhenSearchContainsExtraSpacesOrRolesAreEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        String search = "  Ivan   Petrenko%  ";
        String normalizedSearch = "Ivan Petrenko\\%";

        List<Role> roles = new ArrayList<>();

        Page<User> emptyPage = Page.empty(pageable);

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(repository.findAllBySearchAndRoles(normalizedSearch, null, pageable)).thenReturn(emptyPage);

        Page<ResponseUserDTO> result = userService.getUsers(pageable, search, roles);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(repository).findAllBySearchAndRoles(normalizedSearch, null, pageable);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("getUsers should return all users when search and roles are not provided")
    void getUsers_ShouldReturnAllUsers_WhenSearchAndRolesAreNotProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyPage = Page.empty(pageable);

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(repository.findAllBySearchAndRoles(null, null, pageable)).thenReturn(emptyPage);

        Page<ResponseUserDTO> result = userService.getUsers(pageable, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(repository).findAllBySearchAndRoles(null, null, pageable);
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
    @DisplayName("changeUserStatus should throw UserAuthorizationException when user is not authenticated")
    void changeUserStatus_ShouldThrowUserAuthorizationException_WhenUserIsNotAuthenticated() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeUserStatus(1L, UserStatus.ACTIVE))
            .isInstanceOf(UserAuthorizationException.class)
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

    @Test
    @DisplayName("getUsersByIds should return paged users when current user is admin")
    void getUsersByIds_ShouldReturnPagedUsers_WhenCurrentUserIsAdmin() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Long> ids = List.of(1L, 2L);

        User firstUser = User.builder()
            .id(1L)
            .email("ivan@example.com")
            .firstName("Ivan")
            .surname("Petrenko")
            .middleName("Ivanovych")
            .role(Role.USER)
            .userStatus(UserStatus.ACTIVE)
            .build();

        User secondUser = User.builder()
            .id(2L)
            .email("anna@example.com")
            .firstName("Anna")
            .surname("Kovalenko")
            .middleName("Ivanivna")
            .role(Role.ORG)
            .userStatus(UserStatus.ACTIVE)
            .build();

        ResponseUserDTO firstResponse = ResponseUserDTO.builder()
            .id(1L)
            .email("ivan@example.com")
            .firstName("Ivan")
            .lastName("Petrenko")
            .middleName("Ivanovych")
            .role(Role.USER)
            .status(UserStatus.ACTIVE)
            .build();

        ResponseUserDTO secondResponse = ResponseUserDTO.builder()
            .id(2L)
            .email("anna@example.com")
            .firstName("Anna")
            .lastName("Kovalenko")
            .middleName("Ivanivna")
            .role(Role.ORG)
            .status(UserStatus.ACTIVE)
            .build();

        Page<User> userPage = new PageImpl<>(
            List.of(firstUser, secondUser),
            pageable,
            2);

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(repository.findAllByIdIn(ids, pageable)).thenReturn(userPage);
        when(mapper.toResponseUserDTO(firstUser)).thenReturn(firstResponse);
        when(mapper.toResponseUserDTO(secondUser)).thenReturn(secondResponse);

        Page<ResponseUserDTO> result = userService.getUsersByIds(pageable, ids);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(2L);

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(repository).findAllByIdIn(ids, pageable);
        verify(mapper).toResponseUserDTO(firstUser);
        verify(mapper).toResponseUserDTO(secondUser);
    }

    @Test
    @DisplayName("getUsersByIds should throw InsufficientPermissionsException when current user is not admin")
    void getUsersByIds_ShouldThrowInsufficientPermissionsException_WhenCurrentUserIsNotAdmin() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Long> ids = List.of(1L, 2L);

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(false);

        assertThatThrownBy(() -> userService.getUsersByIds(pageable, ids))
            .isInstanceOf(InsufficientPermissionsException.class)
            .hasMessage("You do not have enough permissions to perform this action");

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("getUsersByIds should return empty page when no users match provided ids")
    void getUsersByIds_ShouldReturnEmptyPage_WhenNoUsersMatchProvidedIds() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Long> ids = List.of(100L, 200L);

        Page<User> emptyPage = Page.empty(pageable);

        when(securityFacade.hasRole(String.valueOf(Role.ADMIN))).thenReturn(true);
        when(repository.findAllByIdIn(ids, pageable)).thenReturn(emptyPage);

        Page<ResponseUserDTO> result = userService.getUsersByIds(pageable, ids);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(securityFacade).hasRole(String.valueOf(Role.ADMIN));
        verify(repository).findAllByIdIn(ids, pageable);
        verifyNoInteractions(mapper);
    }
}