package com.itasocialacademy.oitassist.user.service.interfaces;

import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
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
     * Finds a current authenticated user and returns their profile.
     *
     * @return the user's profile DTO
     * @throws UserAuthorizationException if user is not authenticated
     */
    @NonNull
    ResponseUserDTO getCurrentUserProfile();

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
     * search by name or email.
     *
     * @param pageable pagination parameters
     * @param search   optional search query
     * @return paginated list of users
     * @throws InsufficientPermissionsException if user does not have admin role
     */
    @NonNull
    Page<ResponseUserDTO> getUsers(@NonNull Pageable pageable, String search);

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
