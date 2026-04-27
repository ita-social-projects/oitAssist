package com.itasocialacademy.oitassist.security.api.interfaces;

import org.springframework.modulith.NamedInterface;
import java.util.Optional;

@NamedInterface("SecurityFacade")
public interface SecurityFacade {
    /**
     * Retrieves the email address of the currently authenticated user.
     */
    Optional<String> getCurrentUserEmail();

    /**
     * Retrieves the unique identifier of the current user.
     */
    Optional<Long> getCurrentUserId();

    /**
     * Checks if the authenticated user's ID matches the provided resource's owner
     * ID.
     */
    boolean isOwner(Long ownerId);

    /**
     * Checks if the authenticated user has the specified security role.
     */
    boolean hasRole(String role);
}