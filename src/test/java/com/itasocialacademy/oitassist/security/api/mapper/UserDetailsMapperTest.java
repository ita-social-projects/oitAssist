package com.itasocialacademy.oitassist.security.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.GrantedAuthority;

class UserDetailsMapperTest {
    private static final long USER_ID = 42L;
    private static final String EMAIL = "ivan@example.com";
    private static final String PASSWORD_HASH = "hashed-password";
    private static final Role DOMAIN_ROLE = Role.USER;
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String EXPECTED_AUTHORITY = ROLE_PREFIX + DOMAIN_ROLE.name();
    private static final int EXPECTED_AUTHORITY_COUNT = 1;

    private UserDetailsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserDetailsMapper.class);
    }

    @Test
    void toUserDetails_ShouldCopyAllScalarFields_WhenAuthDetailsProvided() {
        UserAuthDetails authDetails = buildAuthDetails(DOMAIN_ROLE);

        UserDetailsImpl result = mapper.toUserDetails(authDetails);

        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(result.getUsername()).isEqualTo(EMAIL);
    }

    @Test
    void toUserDetails_ShouldWrapRoleInRolePrefixedAuthority_WhenAuthDetailsProvided() {
        UserAuthDetails authDetails = buildAuthDetails(DOMAIN_ROLE);

        UserDetailsImpl result = mapper.toUserDetails(authDetails);

        assertThat(result.getAuthorities())
            .hasSize(EXPECTED_AUTHORITY_COUNT)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly(EXPECTED_AUTHORITY);
    }

    @Test
    void toUserDetails_ShouldMapEachRoleConstantToCorrespondingAuthority_WhenIteratedOverAllRoles() {
        for (Role role : Role.values()) {
            UserAuthDetails authDetails = buildAuthDetails(role);

            UserDetailsImpl result = mapper.toUserDetails(authDetails);

            assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(ROLE_PREFIX + role.name());
        }
    }

    private UserAuthDetails buildAuthDetails(Role role) {
        return UserAuthDetails.builder()
            .id(USER_ID)
            .email(EMAIL)
            .password(PASSWORD_HASH)
            .role(role)
            .build();
    }
}