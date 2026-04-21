package com.itasocialacademy.oitassist.security.api.interfaces;

import org.springframework.modulith.NamedInterface;
import java.util.Optional;

@NamedInterface("SecurityFacade")
public interface SecurityFacade {
    /**
     * Retrieves the email address of the currently authenticated user.
     */
    Optional<String> getCurrentUserEmail();
}