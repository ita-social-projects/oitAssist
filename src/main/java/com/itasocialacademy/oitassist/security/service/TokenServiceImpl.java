package com.itasocialacademy.oitassist.security.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.dao.dto.request.TokenRequest;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorVerifyRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.LoginResponse;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import com.itasocialacademy.oitassist.security.dao.enums.LoginOutcome;
import com.itasocialacademy.oitassist.security.dao.enums.TwoFactorMethod;
import com.itasocialacademy.oitassist.security.dao.model.UserTwoFactorAuth;
import com.itasocialacademy.oitassist.security.dao.repository.UserTwoFactorAuthRepository;
import com.itasocialacademy.oitassist.security.jwt.JwtHelper;
import com.itasocialacademy.oitassist.security.jwt.JwtTokenIssuer;
import com.itasocialacademy.oitassist.security.properties.TwoFactorProperties;
import com.itasocialacademy.oitassist.security.service.interfaces.TokenService;
import com.itasocialacademy.oitassist.security.service.interfaces.TwoFactorService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@inheritDoc}
 *
 * <p>
 * {@code generateToken} now branches into three outcomes after password
 * verification succeeds — see {@link LoginOutcome}. The 2FA-status lookup
 * happens <b>after</b> {@code authenticationManager.authenticate(...)} throws
 * or succeeds, never before: checking it earlier would let an unauthenticated
 * caller learn whether an account has 2FA enabled just by submitting a
 * username, which is a small account-enumeration leak. A failed password
 * attempt looks identical regardless of 2FA status this way.
 * </p>
 *
 * <p>
 * {@code refreshToken} is deliberately untouched by any of this — a refresh
 * token represents an already-established, already-2FA-verified session;
 * requiring 2FA again on every access-token expiry would defeat the point of
 * having a refresh token at all.
 * </p>
 *
 * <p>
 * {@code verifyTwoFactor} is the counterpart that completes a
 * {@code TWO_FA_VERIFICATION_REQUIRED} login: it resolves identity from the
 * pending token (never from {@code SecurityContextHolder}, which isn't
 * populated at this point — there's no ongoing authenticated session yet),
 * delegates the actual code check to {@link TwoFactorService#verify}, and on
 * success re-fetches a full {@link UserDetailsImpl} the same way
 * {@link #refreshToken} already does, before issuing real tokens.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    private final JwtHelper jwtHelper;
    private final JwtTokenIssuer jwtTokenIssuer;

    private final UserTwoFactorAuthRepository twoFactorAuthRepository;
    private final TwoFactorProperties twoFactorProperties;
    private final TwoFactorService twoFactorService;

    @Override
    @Transactional
    public LoginResponse generateToken(TokenRequest tokenRequest) {
        UserDetailsImpl userDetails;
        try {
            userDetails = (UserDetailsImpl) authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    tokenRequest.getUsername(), tokenRequest.getPassword()))
                .getPrincipal();
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Bad credentials", ErrorCode.BAD_CREDENTIAL);
        } catch (DisabledException e) {
            throw new AuthenticationException("Account is not activated", ErrorCode.USER_NOT_ACTIVATED);
        } catch (LockedException e) {
            throw new AuthenticationException("Account is locked", ErrorCode.USER_BLOCKED);
        } catch (AccountExpiredException e) {
            throw new AuthenticationException("Account has expired", ErrorCode.USER_BLOCKED);
        }

        Objects.requireNonNull(userDetails);

        Optional<UserTwoFactorAuth> twoFactorAuth = twoFactorAuthRepository.findByUserId(userDetails.getId());

        if (twoFactorAuth.isPresent() && twoFactorAuth.get().isEnabled()) {
            return buildVerificationRequiredResponse(userDetails, twoFactorAuth.get());
        }

        if (isMandatoryTwoFactorRole(userDetails)) {
            return buildSetupRequiredResponse(userDetails);
        }

        return LoginResponse.builder()
            .outcome(LoginOutcome.SUCCESS)
            .tokens(jwtTokenIssuer.issueFor(userDetails))
            .build();
    }

    @Override
    public TokenResponse refreshToken(String token) {
        String username;
        try {
            String encryptedJwt = jwtHelper.extractEncryptedToken(token);
            username = jwtHelper.extractUsername(encryptedJwt, JwtHelper.REFRESH_TOKEN);
        } catch (SignatureException e) {
            throw new AuthenticationException("Invalid JWT signature", ErrorCode.INVALID_SIGNATURE);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("JWT claims string is empty", ErrorCode.EMPTY_CLAIMS);
        } catch (ExpiredJwtException jwtException) {
            throw new AuthenticationException("User token expire", ErrorCode.TOKEN_EXPIRE);
        } catch (UsernameNotFoundException e) {
            throw new AuthenticationException("Bad credentials", ErrorCode.BAD_CREDENTIAL);
        } catch (UnsupportedJwtException e) {
            throw new AuthenticationException("JWT token is unsupported", ErrorCode.UNSUPPORTED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new AuthenticationException("Invalid JWT token", ErrorCode.INVALID_TOKEN);
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);

        return jwtTokenIssuer.issueFor(userDetails);
    }

    @Override
    public TokenResponse verifyTwoFactor(TwoFactorVerifyRequest request) {
        JwtTokenIssuer.PendingTwoFactorClaims claims = jwtTokenIssuer.readPendingTwoFactorToken(
            request.getPendingTwoFactorToken(), JwtTokenIssuer.PURPOSE_TWO_FACTOR_VERIFY);

        twoFactorService.verify(claims.userId(), request.getCode());

        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(claims.email());

        return jwtTokenIssuer.issueFor(userDetails);
    }

    /**
     * For {@code EMAIL_OTP} users, there's nothing sitting around for them to
     * submit at login — unlike TOTP, where their own authenticator app keeps
     * generating codes independently, an email-OTP user's last code (from whenever
     * they last logged in, if ever) is long since consumed and cleared. So this
     * branch also triggers sending a fresh one via
     * {@link TwoFactorService#resendLoginOtp} before returning the pending token —
     * otherwise the user would have a token to submit but no code to put with it.
     */
    private LoginResponse buildVerificationRequiredResponse(UserDetailsImpl userDetails,
        UserTwoFactorAuth twoFactorAuth) {
        if (twoFactorAuth.getMethod() == TwoFactorMethod.EMAIL_OTP) {
            twoFactorService.resendLoginOtp(userDetails.getId(), userDetails.getEmail());
        }

        String pendingToken = jwtTokenIssuer.issuePendingTwoFactorToken(
            userDetails, JwtTokenIssuer.PURPOSE_TWO_FACTOR_VERIFY,
            twoFactorProperties.getPendingTokenValidityMillis());

        return LoginResponse.builder()
            .outcome(LoginOutcome.TWO_FA_VERIFICATION_REQUIRED)
            .pendingTwoFactorToken(pendingToken)
            .twoFactorMethod(twoFactorAuth.getMethod().name())
            .build();
    }

    private LoginResponse buildSetupRequiredResponse(UserDetailsImpl userDetails) {
        String pendingToken = jwtTokenIssuer.issuePendingTwoFactorToken(
            userDetails, JwtTokenIssuer.PURPOSE_TWO_FACTOR_SETUP,
            twoFactorProperties.getPendingTokenValidityMillis());

        return LoginResponse.builder()
            .outcome(LoginOutcome.TWO_FA_SETUP_REQUIRED)
            .pendingTwoFactorToken(pendingToken)
            .build();
    }

    /**
     * Reads the mandatory-role set straight off the just-authenticated principal's
     * authorities — no {@code SecurityContextHolder} (not populated yet at this
     * point in the flow) and no cross-module import of {@code user.dao.enums.Role}
     * (the {@code security} module isn't allowed to depend on {@code user}; see
     * {@code security/package-info.java}). This mirrors the same "strip ROLE_,
     * compare the bare name" pattern {@link JwtTokenIssuer#issueFor} already uses —
     * a small, acceptable duplication between the two classes rather than a shared
     * utility extracted for two call sites; worth revisiting if a third one shows
     * up.
     */
    private boolean isMandatoryTwoFactorRole(UserDetailsImpl userDetails) {
        return userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(Objects::nonNull)
            .filter(authority -> authority.startsWith(ROLE_PREFIX))
            .map(authority -> authority.substring(ROLE_PREFIX.length()))
            .anyMatch(twoFactorProperties.getMandatoryRoles()::contains);
    }
}