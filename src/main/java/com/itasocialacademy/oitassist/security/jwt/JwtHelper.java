package com.itasocialacademy.oitassist.security.jwt;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtHelper {
    public static final String ACCESS_TOKEN = "access";
    public static final String REFRESH_TOKEN = "refresh";
    public static final String TWO_FACTOR_PENDING_TOKEN = "2fa_pending";

    private final JwtProperties jwtProperties;

    public JwtHelper(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createToken(Map<String, Object> claims, String subject) {
        Date expiryDate = Date.from(Instant.ofEpochMilli(System.currentTimeMillis() + jwtProperties.getValidity()));
        return generateToken(claims, subject, expiryDate);
    }

    public String createRefreshToken(Map<String, Object> claims, String subject) {
        Date expiryDate =
            Date.from(Instant.ofEpochMilli(System.currentTimeMillis() + jwtProperties.getRefreshValidity()));
        return generateToken(claims, subject, expiryDate);
    }

    /**
     * Creates a short-lived token used between password verification and 2FA code
     * verification/setup (see {@link #TWO_FACTOR_PENDING_TOKEN}).
     *
     * <p>
     * Unlike {@link #createToken} and {@link #createRefreshToken}, validity is
     * caller-supplied rather than read from {@link JwtProperties}: this token's
     * lifetime is a 2FA-specific policy concern, not a general JWT concern, so
     * {@link JwtHelper} stays agnostic about what "2FA" means and simply mints
     * whatever expiry it's given.
     * </p>
     *
     * @param claims         the claims to embed; callers are expected to set a
     *                       {@code token_type} claim of
     *                       {@link #TWO_FACTOR_PENDING_TOKEN}
     * @param subject        the token subject (the user's email, matching the
     *                       convention used by {@link #createToken})
     * @param validityMillis how long the token remains valid, in milliseconds
     * @return the signed+encrypted token, ready to return to the client
     */
    public String createTwoFactorPendingToken(Map<String, Object> claims, String subject, long validityMillis) {
        Date expiryDate = Date.from(Instant.ofEpochMilli(System.currentTimeMillis() + validityMillis));
        return generateToken(claims, subject, expiryDate);
    }

    private String generateToken(Map<String, Object> claims, String subject, Date expiryDate) {
        Date currentDate = new Date(System.currentTimeMillis());
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(currentDate)
            .expiration(expiryDate)
            .signWith(getSignKey(), Jwts.SIG.HS384)
            .compact();
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSignKey()));
    }

    public String extractUsername(String token, String tokenType) {
        Claims claims = parseClaims(token);
        if (!tokenType.equals(claims.get("token_type"))) {
            throw new AuthenticationException("Invalid token type", ErrorCode.INVALID_TOKEN_TYPE);
        }
        return claims.getSubject();
    }

    public Claims extractClaims(String token, String expectedTokenType) {
        Claims claims = parseClaims(token);
        if (!expectedTokenType.equals(claims.get("token_type"))) {
            throw new AuthenticationException("Invalid token type", ErrorCode.INVALID_TOKEN_TYPE);
        }
        return claims;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(getSignKey())
            .build().parseSignedClaims(token).getPayload();
    }
}