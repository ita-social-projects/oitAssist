package com.itasocialacademy.oitassist.security.api.interfaces;

import java.util.Optional;
import org.springframework.modulith.NamedInterface;

/**
 * Security interface for managing and retrieving information about the
 * currently authenticated user.
 *
 * @apiNote Contributors should **not** interact with
 *          {@code SecurityContextHolder} directly in business or service
 *          layers. Use this service instead to ensure consistent error
 *          handling, null-safety, and testability across the application.
 */
@NamedInterface("SecurityService")
public interface SecurityService {
    /**
     * Retrieves the unique identifier of the current user.
     *
     * @return an {@link Optional} with the user ID, or empty if unauthenticated.
     */
    Optional<Long> getCurrentUserId();

    /**
     * Retrieves the email of the current user.
     *
     * @return an {@link Optional} with the email string, or empty if
     *         unauthenticated.
     */
    Optional<String> getCurrentUserEmail();

    /**
     * Checks if the authenticated user's ID matches the provided resource's owner
     * ID.
     *
     * @param ownerId the ID to verify ownership against.
     * @return true if the current user is the owner; false otherwise.
     */
    boolean isOwner(Long ownerId);

    /**
     * Checks if the authenticated user has the specified security role.
     *
     * @param role the role name to check (e.g., "ADMIN").
     * @implNote This method automatically handles the "ROLE_" prefix. If the
     *           provided string does not start with "ROLE_", it is prepended
     *           automatically before the authority check is performed.
     * @return true if the user has the role; false otherwise.
     */
    boolean hasRole(String role);
}