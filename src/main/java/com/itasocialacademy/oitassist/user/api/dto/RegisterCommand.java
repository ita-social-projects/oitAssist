package com.itasocialacademy.oitassist.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.springframework.modulith.NamedInterface;

/**
 * Cross-module command DTO used by external modules (e.g. {@code auth}) to
 * register a new user.
 *
 * <p>
 * Validation annotations are declared here as the canonical contract; HTTP
 * layer DTOs that translate into this command (e.g. {@code RegisterRequest})
 * may add their own request-shape validation.
 * </p>
 *
 * <p>
 * The raw password travels in this command; the {@code user} module is
 * responsible for hashing it before persistence.
 * </p>
 */
@Builder
@NamedInterface("RegisterCommand")
public record RegisterCommand(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String firstName,
    @NotBlank String surname,
    @NotBlank String middleName,
    String phoneNumber) {
}
