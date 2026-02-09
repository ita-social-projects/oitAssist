package com.itasocialacademy.oitassist.security.jwt;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.web.GlobalExceptionHandler;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Objects;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final UserDetailsService userDetailsService;
    private final JwtHelper jwtHelper;
    private final GlobalExceptionHandler handler;

    public JwtFilter(UserDetailsService userDetailsService, JwtHelper jwtHelper, GlobalExceptionHandler handler) {
        this.userDetailsService = userDetailsService;
        this.jwtHelper = jwtHelper;
        this.handler = handler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            final String authorizationHeader = request.getHeader(AUTHORIZATION);
            String jwt;
            String username;
            if (Objects.nonNull(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                String encryptedJwt = jwtHelper.extractEncryptedToken(jwt);
                username = jwtHelper.extractUsername(encryptedJwt);

                if (Objects.nonNull(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails =
                        this.userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(
                        usernamePasswordAuthenticationToken);
                }
            }
        } catch (SignatureException e) {
            handler.handleSecurityException(
                new AuthenticationException("Invalid JWT signature", ErrorCode.INVALID_SIGNATURE), request);
        } catch (IllegalArgumentException e) {
            handler.handleSecurityException(
                new AuthenticationException("JWT claims string is empty", ErrorCode.EMPTY_CLAIMS), request);
        } catch (ExpiredJwtException jwtException) {
            handler.handleSecurityException(new AuthenticationException("User token expire", ErrorCode.TOKEN_EXPIRE),
                request);
        } catch (UsernameNotFoundException e) {
            handler.handleSecurityException(new AuthenticationException("Bad credentials", ErrorCode.BAD_CREDENTIAL),
                request);
        } catch (UnsupportedJwtException e) {
            handler.handleSecurityException(
                new AuthenticationException("JWT token is unsupported", ErrorCode.UNSUPPORTED_TOKEN), request);
        } catch (MalformedJwtException e) {
            handler.handleSecurityException(new AuthenticationException("Invalid JWT token", ErrorCode.INVALID_TOKEN),
                request);
        }

        filterChain.doFilter(request, response);
    }
}
