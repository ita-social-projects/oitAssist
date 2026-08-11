package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.security.api.interfaces.SecurityUserProvider;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the {@link SecurityUserProvider} interface.
 *
 * <p>
 * This service serves as a bridge between the User domain module and the
 * Security module. It abstracts the data access logic required to retrieve user
 * credentials and authorities for Spring Security authentication processes.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SecurityUserProviderImpl implements SecurityUserProvider {
    /**
     * Repository for performing CRUD operations on {@link User} entities.
     */
    private final UserRepository userRepository;

    /**
     * Retrieves user security details based on a unique email address.
     *
     * @param email the email address of the user to find.
     * @return an {@link Optional} containing the {@link UserDetailsImpl} if found,
     *         or an empty Optional if no user exists with the given email.
     */
    @Override
    public Optional<UserDetailsImpl> findByEmail(String email) {
        return userRepository.findUserByEmail(email)
            .map(this::mapToUserDetails);
    }

    /**
     * Converts a domain {@link User} entity into a security {@link UserDetailsImpl}
     * DTO.
     *
     * <p>
     * This method maps the user's role to a {@link SimpleGrantedAuthority},
     * prefixing the role name with "ROLE_" as per standard Spring Security
     * conventions.
     * </p>
     *
     * @param user the domain user entity to map.
     * @return a fully populated {@link UserDetailsImpl} object.
     */
    private UserDetailsImpl mapToUserDetails(User user) {
        return UserDetailsImpl.builder()
            .id(user.getId())
            .email(user.getEmail())
            .password(user.getPassword())
            .isEnabled(user.getUserStatus() != UserStatus.PENDING)
            .isAccountNonLocked(user.getUserStatus() != UserStatus.BLOCKED)
            .isAccountNonExpired(user.getUserStatus() != UserStatus.DELETED)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
            .build();
    }
}
