package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.dao.dto.request.ProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.dao.dto.request.ReviewRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import com.itasocialacademy.oitassist.user.dao.model.ProfileUpdateRequest;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.ProfileUpdateRequestRepository;
import com.itasocialacademy.oitassist.user.exceptions.InvalidSortFieldException;
import com.itasocialacademy.oitassist.user.exceptions.ProfileUpdateRequestException;
import com.itasocialacademy.oitassist.user.mapper.ProfileUpdateRequestMapper;
import com.itasocialacademy.oitassist.user.exceptions.*;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import com.itasocialacademy.oitassist.usercompetition.api.interfaces.UserCompetitionFacade;
import jakarta.persistence.EntityNotFoundException;
import org.jetbrains.annotations.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final SecurityFacade securityFacade;
    private final UserRepository repository;
    private final UserMapper mapper;

    private final ProfileUpdateRequestRepository profileUpdateRequestRepository;
    private final UserCompetitionFacade userCompetitionFacade;
    private final ProfileUpdateRequestMapper profileUpdateRequestMapper;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Looks up the user by email and maps to {@link UserAuthDetails} via
     * {@link UserMapper#toUserAuthDetails}. No {@code security}-owned types are
     * involved in this path.
     * </p>
     */
    @Override
    public Optional<UserAuthDetails> findAuthDetailsByEmail(String email) {
        return repository.findUserByEmail(email)
            .map(mapper::toUserAuthDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserAuthDetails> findAuthDetailsByIds(List<Long> userIds) {
        return repository.findAllById(userIds)
            .stream().map(mapper::toUserAuthDetails).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserProfileDetails> findProfileDetailsById(Long userId) {
        return repository.findById(userId)
            .map(mapper::toUserProfileDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserProfileDetails> findProfilesDetailsByIds(List<Long> userIds) {
        return repository.findAllById(userIds)
            .stream().map(mapper::toUserProfileDetails).toList();
    }

    public UserDetailsImpl loadUserByUsername(@NonNull String username) {
        Optional<User> user = repository.findUserByEmail(username);
        return user.map(mapper::toUserDetails).orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseUserDTO loadUserById(Long id) {
        Optional<User> user = repository.findById(id);

        return user.map(mapper::toResponseUserDTO)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ResponseUserDTO loadUserByEmail(@NonNull String email) {
        Optional<User> user = repository.findUserByEmail(email);

        return user.map(mapper::toResponseUserDTO)
            .orElseThrow(UserNotFoundException::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ResponseUserDTO getCurrentUserProfile() {
        String email = securityFacade.getCurrentUserEmail()
            .orElseThrow(UserAuthorizationException::new);

        return loadUserByEmail(email);
    }

    /**
     * Applies profile changes for the user.
     *
     * @param user    current authenticated user
     * @param request new user data
     */
    private void applyProfileUpdate(User user, ProfileUpdateRequest request) {
        user.setFirstName(request.getNewFirstName());
        user.setSurname(request.getNewLastName());
        user.setMiddleName(request.getNewMiddleName());
        user.setPhoneNumber(request.getNewPhoneNumber());
        repository.save(user);
    }

    /**
     * Checks if the user has had any profile update requests during the current
     * day.
     *
     * @param currentUserId the ID of the current authenticated user
     * @return true if the user already submitted a request today, false otherwise
     */
    private boolean hasAnyRequestsToday(Long currentUserId) {
        ZoneId zoneId = ZoneId.of("Europe/Kiev");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        Instant startOfDay = now.toLocalDate().atStartOfDay(zoneId).toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        return profileUpdateRequestRepository.existsByUserIdAndRequestedAtBetween(currentUserId, startOfDay, endOfDay);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void createProfileUpdateRequest(@NotNull ProfileUpdateRequestDTO request) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User is not authenticated", ErrorCode.ACCESS_DENIED));

        User user = repository.findById(currentUserId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUserId));

        if (hasAnyRequestsToday(currentUserId)) {
            throw new ProfileUpdateRequestException("User already had a request today",
                ErrorCode.PROFILE_UPDATE_REQUEST_DAILY_LIMIT);
        }

        boolean hasAnyPendingReq =
            profileUpdateRequestRepository.existsByUserIdAndStatus(currentUserId, UpdateRequestStatus.PENDING);

        if (hasAnyPendingReq) {
            throw new ProfileUpdateRequestException("User already have a pending update request",
                ErrorCode.PROFILE_UPDATE_REQUEST_ALREADY_PENDING);
        }

        boolean hasAnyCompetitions = userCompetitionFacade.hasActiveCompetitions(currentUserId,
            List.of(CompetitionStatus.PUBLISHED, CompetitionStatus.ENROLLMENT));

        UpdateRequestStatus status = hasAnyCompetitions ? UpdateRequestStatus.PENDING : UpdateRequestStatus.APPROVED;

        ProfileUpdateRequest profileUpdateRequest = ProfileUpdateRequest.builder()
            .user(user)
            .status(status)
            .oldFirstName(user.getFirstName())
            .oldLastName(user.getSurname())
            .oldMiddleName(user.getMiddleName())
            .oldPhoneNumber(user.getPhoneNumber())
            .newFirstName(request.getFirstName())
            .newLastName(request.getLastName())
            .newMiddleName(request.getMiddleName())
            .newPhoneNumber(request.getPhoneNumber())
            .requestedAt(Instant.now())
            .build();

        try {
            profileUpdateRequestRepository.save(profileUpdateRequest);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage();

            if (message != null && message.contains("uq_profile_update_requests_per_day")) {
                throw new ProfileUpdateRequestException("User already had a request today",
                    ErrorCode.PROFILE_UPDATE_REQUEST_DAILY_LIMIT);
            }

            if (message != null && message.contains("uq_profile_update_requests_pending")) {
                throw new ProfileUpdateRequestException("User already have a pending update request",
                    ErrorCode.PROFILE_UPDATE_REQUEST_ALREADY_PENDING);
            }

            throw e;
        }

        if (status == UpdateRequestStatus.APPROVED) {
            applyProfileUpdate(user, profileUpdateRequest);
        }
    }

    /**
     * Validates that all sort fields in the given {@link Pageable} are allowed.
     * Permitted sort fields are {@code requestedAt} and {@code status}.
     *
     * @param pageable the pagination and sorting parameters to validate
     * @throws InvalidSortFieldException if any sort field is not in the allowed set
     */
    private void validateSort(Pageable pageable) {
        pageable.getSort().forEach((sortOrder) -> {
            if (!sortOrder.getProperty().equals("requestedAt") && !sortOrder.getProperty().equals("status")) {
                throw new InvalidSortFieldException(sortOrder.getProperty());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ResponseProfileUpdateRequestDTO> getProfileUpdateRequests(UpdateRequestStatus status,
        Pageable pageable) {
        validateSort(pageable);

        Page<ProfileUpdateRequest> result = status == null
            ? profileUpdateRequestRepository.findAll(pageable)
            : profileUpdateRequestRepository.findByStatus(status, pageable);

        return result.map(profileUpdateRequestMapper::toResponseProfileUpdateRequestDTO);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void reviewProfileUpdateRequests(Long id, ReviewRequestDTO body) {
        if (!body.status().equals(UpdateRequestStatus.REJECTED)
            && !body.status().equals(UpdateRequestStatus.APPROVED)) {
            throw new ProfileUpdateRequestException(
                "Status must be APPROVED or REJECTED",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }

        if (body.status().equals(UpdateRequestStatus.REJECTED)
            && (body.rejectReason() == null || body.rejectReason().isBlank())) {
            throw new ProfileUpdateRequestException(
                "Rejection reason cannot be blank",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }

        ProfileUpdateRequest profileUpdateRequest = profileUpdateRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Request not found: " + id));

        if (!profileUpdateRequest.getStatus().equals(UpdateRequestStatus.PENDING)) {
            throw new ProfileUpdateRequestException("Request is already reviewed",
                ErrorCode.PROFILE_UPDATE_REQUEST_ALREADY_REVIEWED);
        }

        if (body.status() == UpdateRequestStatus.REJECTED) {
            profileUpdateRequest.setRejectReason(body.rejectReason());
        }

        profileUpdateRequest.setStatus(body.status());
        profileUpdateRequest.setReviewedAt(Instant.now());
        profileUpdateRequestRepository.save(profileUpdateRequest);

        User user = repository.findById(profileUpdateRequest.getUser().getId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (profileUpdateRequest.getStatus() == UpdateRequestStatus.APPROVED) {
            applyProfileUpdate(user, profileUpdateRequest);
        }
    }

    @Override
    @NonNull
    public ResponseUserDTO changeUserRole(@NonNull Long userId, @NonNull Role newRole) {
        if (securityFacade.getCurrentUserId()
            .orElseThrow(UserAuthorizationException::new)
            .equals(userId)) {
            throw new UserRoleSelfChangeException();
        }
        if (!securityFacade.hasRole(String.valueOf(Role.ADMIN))) {
            throw new InsufficientPermissionsException();
        }
        User user = repository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
        if (user.getRole().equals(Role.ADMIN)) {
            throw new AdminRoleModificationException();
        }
        user.setRole(newRole);
        return mapper.toResponseUserDTO(repository.save(user));
    }

    @Override
    public @NonNull Page<ResponseUserDTO> getUsers(
        @NonNull Pageable pageable,
        String search,
        List<Role> roles) {
        if (!securityFacade.hasRole(String.valueOf(Role.ADMIN))) {
            throw new InsufficientPermissionsException();
        }

        String normalizedSearch = search == null || search.isBlank()
            ? null
            : search.trim()
                .replaceAll("\\s+", " ")
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        List<Role> normalizedRoles = roles == null || roles.isEmpty()
            ? null
            : roles;

        return repository.findAllBySearchAndRoles(
            normalizedSearch,
            normalizedRoles,
            pageable)
            .map(mapper::toResponseUserDTO);
    }

    @Override
    public @NonNull Page<ResponseUserDTO> getUsersByIds(@NonNull Pageable pageable, List<Long> ids) {
        if (!securityFacade.hasRole(String.valueOf(Role.ADMIN))) {
            throw new InsufficientPermissionsException();
        }
        return repository.findAllByIdIn(ids, pageable).map(mapper::toResponseUserDTO);
    }

    @Override
    public @NonNull ResponseUserDTO changeUserStatus(@NonNull Long userId, @NonNull UserStatus newStatus) {
        if (securityFacade.getCurrentUserId()
            .orElseThrow(UserAuthorizationException::new)
            .equals(userId)) {
            throw new UserStatusSelfChangeException();
        }
        if (!securityFacade.hasRole(String.valueOf(Role.ADMIN))) {
            throw new InsufficientPermissionsException();
        }
        User user = repository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
        if (user.getRole().equals(Role.ADMIN)) {
            throw new AdminStatusModificationException();
        }
        user.setUserStatus(newStatus);
        return mapper.toResponseUserDTO(repository.save(user));
    }
}
