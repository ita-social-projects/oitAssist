package com.itasocialacademy.oitassist.user.service.interfaces;

import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.dao.dto.request.ProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.dao.dto.request.ReviewRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import com.itasocialacademy.oitassist.user.exceptions.ProfileUpdateRequestException;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.exceptions.AdminRoleModificationException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import com.itasocialacademy.oitassist.user.exceptions.UserRoleSelfChangeException;
import com.itasocialacademy.oitassist.user.exceptions.UserStatusSelfChangeException;
import com.itasocialacademy.oitassist.user.exceptions.AdminStatusModificationException;
import com.itasocialacademy.oitassist.user.exceptions.UserAuthorizationException;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
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

    /**
     * Similar to the {@link #findAuthDetailsByEmail}, this method looks up a user
     * by their ID and returns the display-side projection required by
     * {@code UserFacade.findProfileById}. Returns empty if no user exists.
     */
    Optional<UserProfileDetails> findProfileDetailsById(Long userId);

    /**
     * Bulk variant of {@link #findProfileDetailsById} — returns the display-side
     * projections for all matching users. IDs with no matching user are simply
     * omitted from the result, consistent with {@link #findAuthDetailsByIds}.
     */
    List<UserProfileDetails> findProfilesDetailsByIds(List<Long> userIds);

    /**
     * Searches for the list of users by their IDs and returns the list of auth-side
     * projections required by {@code UserFacade.findByIds}. Returns empty if no
     * users were found.
     *
     * @param userIds the users' IDs
     * @return the users' auth details DTOs
     */
    List<UserAuthDetails> findAuthDetailsByIds(List<Long> userIds);

    UserDetailsImpl loadUserByUsername(String username);

    /**
     * Finds a user by email and returns their profile.
     *
     * @param email the user's email address
     * @return the user's profile DTO
     * @throws UserNotFoundException if no user found with given email
     */
    @NonNull
    ResponseUserDTO loadUserByEmail(String email);

    /**
     * Finds a user by their ID and returns their profile.
     *
     * @param id the unique identifier of the user
     * @return the user's profile DTO
     * @throws UserNotFoundException if no user is found with the given ID
     */
    ResponseUserDTO loadUserById(Long id);

    /**
     * Finds a current authenticated user and returns their profile.
     *
     * @return the user's profile DTO
     * @throws UserAuthorizationException if user is not authenticated
     */
    @NonNull
    ResponseUserDTO getCurrentUserProfile();

    /**
     * Creates a profile update request for the current authenticated user. If user
     * has active competitions — request goes to PENDING (requires admin approval).
     * If no active competitions — request is auto-approved. User can submit only
     * one profile update request per day.
     *
     * @param request the new profile data
     * @throws AuthorizationException        if user is not authenticated
     * @throws ProfileUpdateRequestException if user already has a pending request
     *                                       or has already submitted a request
     *                                       today
     */
    void createProfileUpdateRequest(@NonNull ProfileUpdateRequestDTO request);

    /**
     * Returns a paginated list of profile update requests, optionally filtered by
     * status. Supports sorting only by {@code requestedAt} and {@code status}
     * fields.
     *
     * @param status   the status to filter by, or {@code null} to return all
     *                 requests
     * @param pageable pagination and sorting parameters(requestedAt or status)
     * @return a page of {@link ResponseProfileUpdateRequestDTO}
     * @throws IllegalArgumentException if sorting is applied on a disallowed field
     */
    Page<ResponseProfileUpdateRequestDTO> getProfileUpdateRequests(UpdateRequestStatus status, Pageable pageable);

    /**
     * Reviews a profile update request by approving or rejecting it. If approved,
     * the user's profile is updated immediately. If rejected, a non-blank rejection
     * reason must be provided. Only requests with {@code PENDING} status can be
     * reviewed.
     *
     * @param id   the unique identifier of the profile update request
     * @param body the review decision containing the new status and optional
     *             rejection reason
     * @throws UserNotFoundException       if no request is found with the given
     *                                       ID, or if the associated user no longer
     *                                       exists
     * @throws ProfileUpdateRequestException if the request has already been
     *                                       reviewed
     * @throws IllegalArgumentException      if the status is not APPROVED or
     *                                       REJECTED, or if the rejection reason is
     *                                       blank when rejecting
     */
    void reviewProfileUpdateRequests(Long id, ReviewRequestDTO body);

    /**
     * Changes the role of an existing user.
     *
     * @param userId  target user identifier
     * @param newRole new role to assign
     * @return updated user profile
     * @throws UserAuthorizationException       if user is not authenticated
     * @throws UserNotFoundException            if user with given id does not exist
     * @throws UserRoleSelfChangeException      if user is trying to change his own
     *                                          role
     * @throws AdminRoleModificationException   if user is trying to change the role
     *                                          of another Admin
     * @throws InsufficientPermissionsException if user does not have admin role
     */
    @NonNull
    ResponseUserDTO changeUserRole(@NonNull Long userId, @NonNull Role newRole);

    /**
     * Returns a paginated list of users for the admin dashboard. Supports optional
     * search by name or email and filter by roles.
     *
     * @param pageable pagination parameters
     * @param search   optional search query
     * @param roles    optional roles filter
     * @return paginated list of users
     * @throws InsufficientPermissionsException if user does not have admin role
     */
    @NonNull
    Page<ResponseUserDTO> getUsers(@NonNull Pageable pageable, String search, List<Role> roles);

    /**
     * Returns a paginated list of users with given ids. Available only for admins
     *
     * @param pageable pagination parameters
     * @param ids      ids to search users by
     * @return paginated list of users
     * @throws InsufficientPermissionsException if user does not have admin role
     */
    @NonNull
    Page<ResponseUserDTO> getUsersByIds(@NonNull Pageable pageable, List<Long> ids);

    /**
     * Changes the status of an existing user.
     *
     * @param userId    target user identifier
     * @param newStatus new status to assign
     * @return updated user profile
     * @throws UserAuthorizationException       if user is not authenticated
     * @throws UserNotFoundException            if user with given id does not exist
     * @throws UserStatusSelfChangeException    if user is trying to change his own
     *                                          status
     * @throws AdminStatusModificationException if user is trying to change the
     *                                          status of another Admin
     * @throws InsufficientPermissionsException if user does not have admin role
     */
    @NonNull
    ResponseUserDTO changeUserStatus(@NonNull Long userId, @NonNull UserStatus newStatus);
}
