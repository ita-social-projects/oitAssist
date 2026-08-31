package com.itasocialacademy.oitassist.security.api.facade;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;

final class StompUserAuthentication extends AbstractAuthenticationToken {
    private final UserDetailsImpl principal;

    StompUserAuthentication(UserDetailsImpl principal) {
        super(Objects.requireNonNull(
            principal,
            "STOMP principal must not be null")
            .getAuthorities());

        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public UserDetailsImpl getPrincipal() {
        return principal;
    }

    /**
     * Spring uses Authentication#getName() as the key for /user destinations.
     */
    @Override
    public String getName() {
        return principal.getId().toString();
    }
}