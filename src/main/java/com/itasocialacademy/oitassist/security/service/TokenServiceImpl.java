package com.itasocialacademy.oitassist.security.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.dao.dto.request.TokenRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import com.itasocialacademy.oitassist.security.jwt.JwtHelper;
import com.itasocialacademy.oitassist.security.jwt.JwtTokenIssuer;
import com.itasocialacademy.oitassist.security.service.interfaces.TokenService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    private final JwtHelper jwtHelper;
    private final JwtTokenIssuer jwtTokenIssuer;

    @Override
    public TokenResponse generateToken(TokenRequest tokenRequest) {
        UserDetailsImpl userDetails;
        try {
            userDetails = (UserDetailsImpl) authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    tokenRequest.getUsername(), tokenRequest.getPassword()))
                .getPrincipal();
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Bad credentials", ErrorCode.BAD_CREDENTIAL);
        } catch (DisabledException e) {
            throw new AuthenticationException("Account is not activated", ErrorCode.USER_NOT_ACTIVATED);
        } catch (LockedException e) {
            throw new AuthenticationException("Account is blocked", ErrorCode.USER_BLOCKED);
        } catch (AccountExpiredException e) {
            throw new AuthenticationException("Account not found", ErrorCode.USER_NOT_FOUND);
        }

        return jwtTokenIssuer.issueFor(Objects.requireNonNull(userDetails));
    }

    @Override
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

        return jwtTokenIssuer.issueFor(userDetails);
    }
}
