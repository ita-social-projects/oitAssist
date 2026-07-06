package com.itasocialacademy.oitassist.user.service.interfaces;

import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.exceptions.AdminRoleModificationException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import com.itasocialacademy.oitassist.user.exceptions.UserRoleSelfChangeException;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * @throws UserNotFoundException if no user found with given email
     */
    @NonNull
    ResponseUserDTO loadUserByEmail(String email);

    /**
     * Finds a current authenticated user and returns their profile.
     *
     * @return the user's profile DTO
     * @throws AuthorizationException if user is not authenticated
     */
    @NonNull
    ResponseUserDTO getCurrentUserProfile();

    /**
     * Changes the role of an existing user.
     *
     * @param userId  target user identifier
     * @param newRole new role to assign
     * @return updated user profile
     * @throws AuthorizationException           if user is not authenticated
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
     * Returns a paginated list of users for the admin dashboard.
     * Supports optional search by user name or email.
     *
     * @param pageable pagination parameters
     * @param search optional search query
     * @return paginated list of users
     */
    @NonNull
    Page<ResponseUserDTO> getUsers(@NonNull Pageable pageable, String search);
}
