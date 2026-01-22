package com.itasocialacademy.oitassist.user.api.interfaces;

import com.itasocialacademy.oitassist.user.api.dto.CreateUserCommand;
import com.itasocialacademy.oitassist.user.api.dto.UserDto;
import com.itasocialacademy.oitassist.user.api.dto.UserPublicDto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("UserFacade")
public interface UserFacade {
    /**
     * Creates a new user.
     * Used by external modules (e.g. Auth, Order).
     */
    UserDto createUser(CreateUserCommand command);

    /**
     * Returns user by id.
     * Read-only operation for other modules.
     */
    UserDto getUserById(UUID userId);

    /**
     * Checks whether a user exists by email.
     * Used for cross-module validation.
     */
    boolean existsByEmail(String email);

    /**
     * Returns public user info (safe for sharing).
     * Should not expose internal fields.
     */
    Optional<UserPublicDto> getPublicUser(UUID userId);

    /**
     * Disables user account.
     * Used by admin or moderation flows.
     */
    void disableUser(UUID userId);
}
