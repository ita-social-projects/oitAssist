package com.itasocialacademy.oitassist.security.api.interfaces;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import java.util.Optional;
import org.springframework.modulith.NamedInterface;

/**
 * Interface for providing user details for security purposes. This interface
 * enables Dependency Inversion, allowing the security module to load user
 * details without depending on the internal implementation of the user module.
 */
@NamedInterface("SecurityUserProvider")
public interface SecurityUserProvider {
    /**
     * Loads user details by email.
     *
     * @param email the email of the user to load.
     * @return an Optional containing the user details if found, or empty if not.
     */
    Optional<UserDetailsImpl> findByEmail(String email);
}
