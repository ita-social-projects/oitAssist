package com.itasocialacademy.oitassist.security.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.jwt.JwtHelper;
import com.itasocialacademy.oitassist.security.service.interfaces.TokenService;
import com.itasocialacademy.oitassist.security.dao.dto.request.TokenRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class TokenServiceImpl implements TokenService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    private final JwtHelper jwtHelper;

    public TokenServiceImpl(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
        JwtHelper jwtHelper) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtHelper = jwtHelper;
    }

    public TokenResponse generateToken(TokenRequest tokenRequest) {
        UserDetailsImpl userDetails;
        try {
            userDetails = (UserDetailsImpl) this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    tokenRequest.getUsername(), tokenRequest.getPassword()))
                .getPrincipal();
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Bad credentials", ErrorCode.BAD_CREDENTIAL);
        }

        return getTokenResponse(Objects.requireNonNull(userDetails));
    }

    public TokenResponse refreshToken(String token) {
        String username;
        try {
            String encryptedJwt = jwtHelper.extractEncryptedToken(token);
            username = jwtHelper.extractUsername(encryptedJwt, JwtHelper.REFRESH_TOKEN);
        } catch (SignatureException e) {
            throw new AuthenticationException("Invalid JWT signature", ErrorCode.INVALID_SIGNATURE);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("JWT claims string is empty", ErrorCode.EMPTY_CLAIMS);
        } catch (ExpiredJwtException jwtException) {
            throw new AuthenticationException("User token expire", ErrorCode.TOKEN_EXPIRE);
        } catch (UsernameNotFoundException e) {
            throw new AuthenticationException("Bad credentials", ErrorCode.BAD_CREDENTIAL);
        } catch (UnsupportedJwtException e) {
            throw new AuthenticationException("JWT token is unsupported", ErrorCode.UNSUPPORTED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new AuthenticationException("Invalid JWT token", ErrorCode.INVALID_TOKEN);
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);

        return getTokenResponse(userDetails);
    }

    private TokenResponse getTokenResponse(UserDetailsImpl userDetails) {
        Map<String, Object> accessTokenClaims = new HashMap<>();
        accessTokenClaims.put("id", userDetails.getId());

        // Extract role from authorities (assuming "ROLE_XXX" format and single role)
        String role = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(Objects::nonNull)
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.substring(5))
            .findFirst()
            .orElse("");
        accessTokenClaims.put("role", role);

        accessTokenClaims.put("token_type", JwtHelper.ACCESS_TOKEN);
        String accessToken = jwtHelper.createToken(accessTokenClaims, userDetails.getUsername());

        Map<String, Object> refreshTokenClaims = new HashMap<>();
        refreshTokenClaims.put("token_type", JwtHelper.REFRESH_TOKEN);
        String refreshToken = jwtHelper.createRefreshToken(refreshTokenClaims, userDetails.getUsername());

        return TokenResponse.builder()
            .token(accessToken)
            .refreshToken(refreshToken)
            .build();
    }
}
