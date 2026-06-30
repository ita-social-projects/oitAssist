package com.itasocialacademy.oitassist.security.api.dto;

import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * An immutable DTO representing the user identity within the security context.
 * This class serves as a "Passport" (data carrier) and is independent of JPA
 * entities.
 */
@Getter
@Builder
@NamedInterface("UserDetailsImpl")
public class UserDetailsImpl implements UserDetails {
    private final Long id;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String email, String password, boolean enabled,
        Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.authorities =
            authorities != null ? Collections.unmodifiableCollection(authorities) : Collections.emptyList();
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public @NonNull String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDetailsImpl that = (UserDetailsImpl) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserDetailsImpl{"
            + "id="
            + id
            + ", email='"
            + email
            + '\''
            + ", authorities="
            + authorities
            + '}';
    }
}
