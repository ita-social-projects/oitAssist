package com.itasocialacademy.oitassist.security.jwt;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import com.itasocialacademy.oitassist.core.web.GlobalExceptionHandler;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final JwtHelper jwtHelper;
    private final GlobalExceptionHandler handler;
    private final ObjectMapper objectMapper;

    public JwtFilter(JwtHelper jwtHelper, GlobalExceptionHandler handler,
        ObjectMapper objectMapper) {
        this.jwtHelper = jwtHelper;
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            final String authorizationHeader = request.getHeader(AUTHORIZATION);
            if (Objects.nonNull(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
                String jwt = authorizationHeader.substring(7);
                Claims claims = jwtHelper.extractClaims(jwt, JwtHelper.ACCESS_TOKEN);

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetailsImpl userDetails = buildUserDetails(claims);
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
            setError(request, response, "Invalid JWT signature", ErrorCode.INVALID_SIGNATURE);
            return;
        } catch (IllegalArgumentException e) {
            setError(request, response, "JWT claims string is empty", ErrorCode.EMPTY_CLAIMS);
            return;
        } catch (ExpiredJwtException jwtException) {
            setError(request, response, "User token expire", ErrorCode.TOKEN_EXPIRE);
            return;
        } catch (UnsupportedJwtException e) {
            setError(request, response, "JWT token is unsupported", ErrorCode.UNSUPPORTED_TOKEN);
            return;
        } catch (MalformedJwtException e) {
            setError(request, response, "Invalid JWT token", ErrorCode.INVALID_TOKEN);
            return;
        } catch (AuthenticationException e) {
            setError(request, response, e.getMessage(), e.getErrorCode());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Builds the authenticated principal directly from the access token's claims,
     * with no database lookup. {@code isEnabled}/{@code isAccountNonLocked}/
     * {@code isAccountNonExpired} are not carried by the token, so they're assumed
     * {@code true} for the lifetime of the access token — the account was in good
     * standing when the token was issued. A ban, lock, or deactivation takes effect
     * only once this token expires and the user is forced through refresh/re-login,
     * not immediately. See issue #565 for the accepted trade-off.
     */
    private UserDetailsImpl buildUserDetails(Claims claims) {
        Long id = claims.get("id", Long.class);
        String role = claims.get("role", String.class);
        return UserDetailsImpl.builder()
            .id(id)
            .email(claims.getSubject())
            .isEnabled(true)
            .isAccountNonLocked(true)
            .isAccountNonExpired(true)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role)))
            .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/v1/security/refresh")
            || path.startsWith("/oauth2/")
            || path.startsWith("/login/oauth2/");
    }

    private void setError(HttpServletRequest request, HttpServletResponse response, String message, ErrorCode errorCode)
        throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ErrorResponse errorResponse =
            handler.handleSecurityException(new AuthenticationException(message, errorCode), request).getBody();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
}
