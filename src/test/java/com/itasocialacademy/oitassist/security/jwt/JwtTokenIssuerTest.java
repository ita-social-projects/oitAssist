package com.itasocialacademy.oitassist.security.jwt;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenIssuerTest {
    private static final long USER_ID = 42L;
    private static final String EMAIL = "ivan@example.com";
    private static final String PASSWORD_HASH = "hashed-password";
    private static final String ROLE_NAME = "USER";
    private static final String ROLE_AUTHORITY = "ROLE_" + ROLE_NAME;
    private static final String NON_ROLE_AUTHORITY = "READ_ONLY";
    private static final String EMPTY_ROLE = "";
    private static final String STUBBED_ACCESS_JWT = "stubbed-access-jwt";
    private static final String STUBBED_REFRESH_JWT = "stubbed-refresh-jwt";
    private static final String NULL_ID_ERROR_FRAGMENT = "user id must not be null";

    @Mock
    private JwtHelper jwtHelper;
    @InjectMocks
    private JwtTokenIssuer jwtTokenIssuer;

    private UserDetailsImpl user;

    @BeforeEach
    void setUp() {
        user = buildUser(USER_ID, ROLE_AUTHORITY);
    }

    @Test
    void issueFor_ShouldReturnTokenResponseWithBothTokens_WhenCalledWithValidUserDetails() {
        stubTokenCreation();

        TokenResponse response = jwtTokenIssuer.issueFor(user);

        assertThat(response.getToken()).isEqualTo(STUBBED_ACCESS_JWT);
        assertThat(response.getRefreshToken()).isEqualTo(STUBBED_REFRESH_JWT);
    }

    @Test
    void issueFor_ShouldIncludeIdRoleAndAccessTypeClaims_WhenBuildingAccessToken() {
        stubTokenCreation();

        jwtTokenIssuer.issueFor(user);

        assertThat(captureAccessTokenClaims())
            .containsEntry(JwtTokenIssuer.CLAIM_ID, USER_ID)
            .containsEntry(JwtTokenIssuer.CLAIM_ROLE, ROLE_NAME)
            .containsEntry(JwtTokenIssuer.CLAIM_TOKEN_TYPE, JwtHelper.ACCESS_TOKEN);
    }

    @Test
    void issueFor_ShouldIncludeOnlyTokenTypeClaim_WhenBuildingRefreshToken() {
        stubTokenCreation();

        jwtTokenIssuer.issueFor(user);

        assertThat(captureRefreshTokenClaims())
            .containsEntry(JwtTokenIssuer.CLAIM_TOKEN_TYPE, JwtHelper.REFRESH_TOKEN)
            .doesNotContainKey(JwtTokenIssuer.CLAIM_ID)
            .doesNotContainKey(JwtTokenIssuer.CLAIM_ROLE);
    }

    @Test
    void issueFor_ShouldSetEmptyRoleClaim_WhenAuthoritiesContainNoRolePrefix() {
        UserDetailsImpl userWithoutRole = buildUser(USER_ID, NON_ROLE_AUTHORITY);
        stubTokenCreation();

        jwtTokenIssuer.issueFor(userWithoutRole);

        assertThat(captureAccessTokenClaims())
            .containsEntry(JwtTokenIssuer.CLAIM_ROLE, EMPTY_ROLE);
    }

    @Test
    void issueFor_ShouldThrowNullPointerException_WhenUserIdIsNull() {
        UserDetailsImpl userWithoutId = buildUser(null, ROLE_AUTHORITY);

        assertThatNullPointerException()
            .isThrownBy(() -> jwtTokenIssuer.issueFor(userWithoutId))
            .withMessageContaining(NULL_ID_ERROR_FRAGMENT);
    }

    private UserDetailsImpl buildUser(Long id, String authority) {
        return UserDetailsImpl.builder()
            .id(id)
            .email(EMAIL)
            .password(PASSWORD_HASH)
            .authorities(List.of(new SimpleGrantedAuthority(authority)))
            .build();
    }

    private void stubTokenCreation() {
        when(jwtHelper.createToken(any(), eq(EMAIL))).thenReturn(STUBBED_ACCESS_JWT);
        when(jwtHelper.createRefreshToken(any(), eq(EMAIL))).thenReturn(STUBBED_REFRESH_JWT);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureAccessTokenClaims() {
        ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
        verify(jwtHelper).createToken(claims.capture(), eq(EMAIL));
        return claims.getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureRefreshTokenClaims() {
        ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
        verify(jwtHelper).createRefreshToken(claims.capture(), eq(EMAIL));
        return claims.getValue();
    }
}