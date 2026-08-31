package com.itasocialacademy.oitassist.security.api.facade;

import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityUserProvider;
import com.itasocialacademy.oitassist.security.api.interfaces.StompAuthenticationFacade;
import com.itasocialacademy.oitassist.security.jwt.JwtHelper;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StompAuthenticationFacadeImpl
        implements StompAuthenticationFacade {
    private static final String INVALID_AUTHENTICATION =
            "Invalid STOMP authentication";

    private final JwtHelper jwtHelper;
    private final SecurityUserProvider securityUserProvider;

    @Override
    public Authentication authenticateAccessToken(
            String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw invalidAuthentication();
        }

        String username = extractUsername(accessToken);

        if (username == null || username.isBlank()) {
            throw invalidAuthentication();
        }

        UserDetailsImpl userDetails = securityUserProvider
                .findByEmail(username)
                .orElseThrow(
                        StompAuthenticationFacadeImpl::invalidAuthentication);

        validateUserDetails(userDetails);

        return new StompUserAuthentication(userDetails);
    }

    private String extractUsername(String accessToken) {
        try {
            String encryptedToken =
                    jwtHelper.extractEncryptedToken(accessToken);

            return jwtHelper.extractUsername(
                    encryptedToken,
                    JwtHelper.ACCESS_TOKEN);
        } catch (JwtException
                 | IllegalArgumentException
                 | AuthenticationException exception) {
            throw invalidAuthentication();
        }
    }

    private void validateUserDetails(
            UserDetailsImpl userDetails) {
        boolean valid = userDetails.getId() != null
                && userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();

        if (!valid) {
            throw invalidAuthentication();
        }
    }

    private static BadCredentialsException invalidAuthentication() {
        return new BadCredentialsException(
                INVALID_AUTHENTICATION);
    }
}