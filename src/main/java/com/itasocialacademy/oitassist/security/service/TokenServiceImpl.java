package com.itasocialacademy.oitassist.security.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.jwt.JwtHelper;
import com.itasocialacademy.oitassist.security.service.interfaces.TokenService;
import com.itasocialacademy.oitassist.security.dao.dto.request.TokenRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import com.itasocialacademy.oitassist.user.api.dto.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenServiceImpl implements TokenService {
    private final AuthenticationManager authenticationManager;

    private final JwtHelper jwtHelper;

    public TokenServiceImpl(AuthenticationManager authenticationManager,
        JwtHelper jwtHelper) {
        this.authenticationManager = authenticationManager;
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
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userDetails.getId());
        claims.put("role", userDetails.getRole());
        String token = jwtHelper.createToken(
            claims, userDetails.getUsername());
        return TokenResponse.builder()
            .token(token)
            .build();
    }
}
