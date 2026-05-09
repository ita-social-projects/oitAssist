package com.itasocialacademy.oitassist.user.api.dto;

import com.itasocialacademy.oitassist.user.dao.enums.Role;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.springframework.modulith.NamedInterface;

/**
 * Authentication-side projection of a {@code User}, exposed to the
 * {@code security} module so it can build its own {@code UserDetails}
 * representation.
 *
 * <p>
 * This DTO carries the encoded password — the {@code security} module needs it
 * to delegate password verification to Spring Security's
 * {@code AuthenticationManager}. It must never be returned to HTTP clients or
 * leak outside the {@code user}↔{@code security} boundary.
 * </p>
 *
 * <p>
 * {@code Role} is exposed as an enum (not as a {@code GrantedAuthority}) to
 * keep this DTO free of Spring Security types. Mapping to authorities happens
 * inside {@code security}.
 * </p>
 */
@Builder
@NamedInterface("UserAuthDetails")
public record UserAuthDetails(
    Long id,
    String email,
    String password,
    Role role) {
    @Override
    @NonNull
    public String toString() {
        return "UserAuthDetails[id=" + id + ", email=" + email + ", password=REDACTED, role=" + role + "]";
    }
}
