package com.itasocialacademy.oitassist.users.service;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.service.SecurityUserProviderImpl;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit test for SecurityUserProviderImpl")
class SecurityUserProviderImplTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityUserProviderImpl securityUserProvider;

    @Test
    @DisplayName("findByEmail should return mapped user details when user exists")
    void findByEmail_ShouldReturnUserDetails_WhenUserExists() {
        String email = "ivan@example.com";

        User user = User.builder()
            .id(1L)
            .email(email)
            .password("encoded-password")
            .role(Role.USER)
            .userStatus(UserStatus.ACTIVE)
            .build();

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));

        Optional<UserDetailsImpl> result = securityUserProvider.findByEmail(email);

        assertThat(result).isPresent();
        UserDetailsImpl details = result.get();

        assertThat(details.getId()).isEqualTo(1L);
        assertThat(details.getEmail()).isEqualTo(email);
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");

        verify(userRepository).findUserByEmail(email);
    }

    @Test
    @DisplayName("findByEmail should return empty Optional when user does not exist")
    void findByEmail_ShouldReturnEmptyOptional_WhenUserNotFound() {
        String email = "unknown@example.com";

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.empty());

        Optional<UserDetailsImpl> result = securityUserProvider.findByEmail(email);

        assertThat(result).isEmpty();

        verify(userRepository).findUserByEmail(email);
    }

    @ParameterizedTest(name = "{0}: enabled={1}, nonLocked={2}, nonExpired={3}")
    @CsvSource({
        "ACTIVE,  true,  true,  true",
        "PENDING, false, true,  true",
        "BLOCKED, true,  false, true",
        "DELETED, true,  true,  false"
    })
    @DisplayName("findByEmail should map account flags according to user status")
    void findByEmail_ShouldMapAccountFlags_WhenUserHasGivenStatus(
        UserStatus status, boolean enabled, boolean nonLocked, boolean nonExpired) {
        String email = "ivan@example.com";

        User user = User.builder()
            .email(email)
            .role(Role.USER)
            .userStatus(status)
            .build();

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));

        UserDetailsImpl details = securityUserProvider.findByEmail(email).orElseThrow();

        assertThat(details.isEnabled()).isEqualTo(enabled);
        assertThat(details.isAccountNonLocked()).isEqualTo(nonLocked);
        assertThat(details.isAccountNonExpired()).isEqualTo(nonExpired);
    }
}
