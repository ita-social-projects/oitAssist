package com.itasocialacademy.oitassist.user.service.interfaces;

import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.ProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.exceptions.ProfileUpdateRequestException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface UserService extends BaseService<Long, CreateUserDTO, UpdateUserDTO, ResponseUserDTO> {
    /**
     * Looks up a user by email and returns the auth-side projection required by
     * {@code UserFacade.findByEmail}. Returns empty if no user exists.
     *
     * <p>
     * This is the canonical lookup path for the {@code security} module from Phase
     * 1.3 onward. It replaces the pre-refactor {@link #loadUserByUsername} path
     * which returned a {@code security}-owned type directly.
     * </p>
     */
    Optional<UserAuthDetails> findAuthDetailsByEmail(String email);

    UserDetailsImpl loadUserByUsername(String username);

    /**
     * Finds a user by email and returns their profile.
     *
     * @param email the user's email address
     * @return the user's profile DTO
     * @throws EntityNotFoundException if no user found with given email
     */
    @NonNull
    ResponseUserDTO loadUserByEmail(String email);

    /**
     * Finds a current authenticated user and returns their profile.
     *
     * @return the user's profile DTO
     * @throws EntityNotFoundException if no user found with given email
     */
    @NonNull
    ResponseUserDTO getCurrentUserProfile();

    /**
     * Creates a profile update request for the current authenticated user.
     * If user has active competitions — request goes to PENDING (requires admin approval).
     * If no active competitions — request is auto-approved.
     * User can submit only one profile update request per day.
     *
     * @param request the new profile data
     * @throws AuthorizationException if user is not authenticated
     * @throws ProfileUpdateRequestException if user already has a pending request or has already submitted a request today
     */
    void createProfileUpdateRequest(@NonNull ProfileUpdateRequestDTO request);
}
