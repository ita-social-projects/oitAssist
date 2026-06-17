package com.itasocialacademy.oitassist.security.jwt;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
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
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Objects;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final UserDetailsService userDetailsService;
    private final JwtHelper jwtHelper;
    private final GlobalExceptionHandler handler;
    private final ObjectMapper objectMapper;

    public JwtFilter(UserDetailsService userDetailsService, JwtHelper jwtHelper, GlobalExceptionHandler handler,
        ObjectMapper objectMapper) {
        this.userDetailsService = userDetailsService;
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
            String jwt;
            String username;
            if (Objects.nonNull(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                String encryptedJwt = jwtHelper.extractEncryptedToken(jwt);
                username = jwtHelper.extractUsername(encryptedJwt, JwtHelper.ACCESS_TOKEN);

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
            setError(request, response, "Invalid JWT signature", ErrorCode.INVALID_SIGNATURE);
            return;
        } catch (IllegalArgumentException e) {
            setError(request, response, "JWT claims string is empty", ErrorCode.EMPTY_CLAIMS);
            return;
        } catch (ExpiredJwtException jwtException) {
            setError(request, response, "User token expire", ErrorCode.TOKEN_EXPIRE);
            return;
        } catch (UsernameNotFoundException e) {
            setError(request, response, "Bad credentials", ErrorCode.BAD_CREDENTIAL);
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
