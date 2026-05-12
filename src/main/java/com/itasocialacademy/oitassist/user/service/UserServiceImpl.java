package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.rest.service.AbstractServiceImpl;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.ProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import com.itasocialacademy.oitassist.user.dao.model.ProfileUpdateRequest;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.ProfileUpdateRequestRepository;
import com.itasocialacademy.oitassist.user.exceptions.ProfileUpdateRequestException;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import com.itasocialacademy.oitassist.usercompetition.api.interfaces.UserCompetitionFacade;
import jakarta.persistence.EntityNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl
    extends AbstractServiceImpl<Long, User, CreateUserDTO, UpdateUserDTO, ResponseUserDTO, UserRepository, UserMapper>
    implements UserService {
    private final SecurityFacade securityFacade;
    private final ProfileUpdateRequestRepository profileUpdateRequestRepository;
    private final UserCompetitionFacade userCompetitionFacade;

    protected UserServiceImpl(UserRepository repository, UserMapper mapper, SecurityFacade securityFacade,
        ProfileUpdateRequestRepository profileUpdateRequestRepository, UserCompetitionFacade userCompetitionFacade) {
        super(repository, mapper);
        this.securityFacade = securityFacade;
        this.profileUpdateRequestRepository = profileUpdateRequestRepository;
        this.userCompetitionFacade = userCompetitionFacade;
    }

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

    public UserDetailsImpl loadUserByUsername(@NonNull String username) {
        Optional<User> user = repository.findUserByEmail(username);
        return user.map(mapper::toUserDetails).orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ResponseUserDTO loadUserByEmail(@NonNull String email) {
        Optional<User> user = repository.findUserByEmail(email);

        return user.map(mapper::toResponseUserDTO)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ResponseUserDTO getCurrentUserProfile() {
        String email = securityFacade.getCurrentUserEmail()
            .orElseThrow(() -> new AuthorizationException("User is not authenticated", ErrorCode.ACCESS_DENIED));

        return loadUserByEmail(email);
    }

    /**
     * Applies profile changes for the user.
     *
     * @param user    current authenticated user
     * @param request new user data
     */
    private void applyProfileUpdate(User user, ProfileUpdateRequestDTO request) {
        user.setFirstName(request.getFirstName());
        user.setSurname(request.getLastName());
        user.setMiddleName(request.getMiddleName());
        user.setPhoneNumber(request.getPhoneNumber());
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
            List.of(CompetitionStatus.INCOMING, CompetitionStatus.INPROGRESS));

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
            throw new ProfileUpdateRequestException("User already have a pending update request",
                ErrorCode.PROFILE_UPDATE_REQUEST_ALREADY_PENDING);
        }

        if (status == UpdateRequestStatus.APPROVED) {
            applyProfileUpdate(user, request);
        }
    }
}
