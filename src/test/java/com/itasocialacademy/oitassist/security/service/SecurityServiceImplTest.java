package com.itasocialacademy.oitassist.security.service;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceImplTest {

    @InjectMocks
    private SecurityServiceImpl securityService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // --- getPrincipal Tests ---

    @Test
    void getCurrentUserId_shouldReturnId_whenAuthenticated() {
        mockPrincipal(1L, "test@mail.com");

        Optional<Long> result = securityService.getCurrentUserId();

        assertTrue(result.isPresent());
        assertEquals(1L, result.get());
    }

    @Test
    void getCurrentUserId_shouldReturnEmpty_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        Optional<Long> result = securityService.getCurrentUserId();

        assertFalse(result.isPresent());
    }

    @Test
    void getCurrentUserId_shouldReturnEmpty_whenPrincipalIsNotUserDetails() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        // Mocking a principal that is a String instead of UserDetailsImpl
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        SecurityContextHolder.setContext(securityContext);

        Optional<Long> result = securityService.getCurrentUserId();

        assertFalse(result.isPresent());
    }

    @Test
    void getCurrentUserEmail_shouldReturnEmail_whenAuthenticated() {
        mockPrincipal(1L, "test@mail.com");

        Optional<String> result = securityService.getCurrentUserEmail();

        assertTrue(result.isPresent());
        assertEquals("test@mail.com", result.get());
    }

    @Test
    void getCurrentUserEmail_shouldReturnEmpty_whenNotAuthenticated() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        SecurityContextHolder.setContext(securityContext);

        Optional<String> result = securityService.getCurrentUserEmail();

        assertFalse(result.isPresent());
    }

    // --- isOwner Tests ---

    @Test
    void isOwner_shouldReturnTrue_whenIdMatches() {
        mockPrincipal(1L, "test@mail.com");

        boolean result = securityService.isOwner(1L);

        assertTrue(result);
    }

    @Test
    void isOwner_shouldReturnFalse_whenIdDoesNotMatch() {
        mockPrincipal(1L, "test@mail.com");

        boolean result = securityService.isOwner(2L);

        assertFalse(result);
    }

    @Test
    void isOwner_shouldReturnFalse_whenOwnerIdIsNull() {
        boolean result = securityService.isOwner(null);

        assertFalse(result);
    }

    // --- Role Tests ---

    @Test
    void hasRole_shouldReturnTrue_whenRoleMatchesWithPrefix() {
        mockAuthorities("ROLE_USER");

        boolean result = securityService.hasRole("USER");

        assertTrue(result);
    }

    @Test
    void hasRole_shouldReturnTrue_whenRoleMatchesWithoutPrefix() {
        mockAuthorities("ROLE_ADMIN");

        boolean result = securityService.hasRole("ROLE_ADMIN");

        assertTrue(result);
    }

    @Test
    void hasRole_shouldReturnFalse_whenRoleDoesNotMatch() {
        mockAuthorities("ROLE_USER");

        boolean result = securityService.hasRole("ADMIN");

        assertFalse(result);
    }

    @Test
    void hasRole_shouldReturnFalse_whenRoleIsNullOrEmpty() {
        assertFalse(securityService.hasRole(null));
        assertFalse(securityService.hasRole("   "));
    }

    @Test
    void hasRole_shouldReturnFalse_whenAuthenticationIsNull() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        boolean result = securityService.hasRole("USER");

        assertFalse(result);
    }

    // --- Service methods ---

    private void mockPrincipal(Long id, String email) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        UserDetailsImpl user = UserDetailsImpl.builder()
            .id(id)
            .email(email)
            .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContextHolder.setContext(securityContext);
    }

    private void mockAuthorities(String... roles) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        Collection<GrantedAuthority> authorities = Arrays.stream(roles)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(authorities).when(authentication).getAuthorities();

        SecurityContextHolder.setContext(securityContext);
    }
}