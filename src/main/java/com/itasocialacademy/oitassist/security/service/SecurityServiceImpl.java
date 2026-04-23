package com.itasocialacademy.oitassist.security.service;

import com.itasocialacademy.oitassist.security.service.interfaces.SecurityService;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Concrete implementation of {@link SecurityService} that interacts with the
 * Spring Security context to provide information about the currently
 * authenticated user.
 *
 * <p>
 * This service acts as a wrapper around {@link SecurityContextHolder}, offering
 * a null-safe and type-safe way to access user details and perform
 * authorization checks.
 * </p>
 */
@Service
public class SecurityServiceImpl implements SecurityService {
    /**
     * Standard prefix used by Spring Security for role-based authorities.
     */
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Retrieves the unique identifier of the currently authenticated user.
     *
     * @return an {@link Optional} containing the user ID if the user is
     *         authenticated and the principal is of type {@link UserDetailsImpl},
     *         otherwise empty.
     */
    @Override
    public Optional<Long> getCurrentUserId() {
        return getPrincipal().map(UserDetailsImpl::getId);
    }

    /**
     * Retrieves the email address of the currently authenticated user.
     *
     * @return an {@link Optional} containing the email if available, otherwise
     *         empty.
     */
    @Override
    public Optional<String> getCurrentUserEmail() {
        return getPrincipal().map(UserDetailsImpl::getEmail);
    }

    /**
     * Checks if the currently authenticated user is the owner of a specific
     * resource.
     *
     * @param ownerId the ID to compare against the current user's ID.
     * @return true if the provided ID matches the current authenticated user's ID;
     *         false if IDs do not match, the provided ID is null, or the user is
     *         not authenticated.
     */
    @Override
    public boolean isOwner(Long ownerId) {
        return ownerId != null && getCurrentUserId()
            .filter(id -> id.equals(ownerId))
            .isPresent();
    }

    /**
     * Checks if the currently authenticated user possesses a specific security
     * role.
     *
     * <p>
     * This method automatically handles the "ROLE_" prefix. If the provided role
     * string does not start with "ROLE_", it will be prepended before checking
     * authorities.
     * </p>
     *
     * @param role the name of the role to check (e.g., "ADMIN" or "ROLE_ADMIN").
     * @return true if the user is authenticated and has the specified role; false
     *         otherwise.
     */
    @Override
    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }

        final String targetRole = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;

        return getAuthentication()
            .map(auth -> auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(targetRole::equals))
            .orElse(false);
    }

    /**
     * Centralized retrieval of the {@link Authentication} object from the
     * {@link SecurityContextHolder}.
     *
     * @return an {@link Optional} containing the authentication object if the user
     *         is authenticated, otherwise empty.
     */
    private Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(Authentication::isAuthenticated);
    }

    /**
     * Internal helper to extract and cast the principal from the security context.
     *
     * @return an {@link Optional} containing the cast {@link UserDetailsImpl}
     *         principal.
     */
    private Optional<UserDetailsImpl> getPrincipal() {
        return getAuthentication()
            .map(Authentication::getPrincipal)
            .filter(UserDetailsImpl.class::isInstance)
            .map(UserDetailsImpl.class::cast);
    }
}
