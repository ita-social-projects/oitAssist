package com.itasocialacademy.oitassist.security.jwt;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.exceptions.AlgorithmNotFoundException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

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
        String encryptedToken = Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(currentDate)
            .expiration(expiryDate)
            .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
            .compact();
        return Jwts.builder()
            .subject(encryptedToken)
            .issuedAt(currentDate)
            .expiration(expiryDate)
            .signWith(getSignKey(), Jwts.SIG.HS384)
            .compact();
    }

    public String extractUsername(String token, String tokenType) {
        Jwe<Claims> jwe = extractEncryptedClaims(token);
        if (!tokenType.equals(jwe.getPayload().get("token_type"))) {
            throw new AuthenticationException("Invalid token type", ErrorCode.INVALID_TOKEN_TYPE);
        }
        return extractClaimBody(jwe, Claims::getSubject);
    }

    /**
     * Decrypts an already-unwrapped token (see {@link #extractEncryptedToken}) and
     * returns its full claim set, after verifying it carries the expected
     * {@code token_type}.
     *
     * <p>
     * Unlike {@link #extractUsername}, which only returns the subject, this exposes
     * every claim — needed by callers such as the 2FA verify/setup endpoints that
     * read additional claims (e.g. {@code purpose}, {@code id}) beyond just the
     * username.
     * </p>
     *
     * @param token             the encrypted (JWE) token, already unwrapped from
     *                          its outer signature via
     *                          {@link #extractEncryptedToken}
     * @param expectedTokenType the {@code token_type} this token must carry
     * @return the decrypted claim set
     * @throws AuthenticationException if the token's {@code token_type} does not
     *                                 match {@code expectedTokenType}
     */
    public Claims extractClaims(String token, String expectedTokenType) {
        Jwe<Claims> jwe = extractEncryptedClaims(token);
        Claims claims = jwe.getPayload();
        if (!expectedTokenType.equals(claims.get("token_type"))) {
            throw new AuthenticationException("Invalid token type", ErrorCode.INVALID_TOKEN_TYPE);
        }
        return claims;
    }

    public <T> T extractClaimBody(ProtectedJwt<?, Claims> jwsClaims,
        Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(jwsClaims.getPayload());
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSignKey()));
    }

    private SecretKey getEncryptionKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getEncryptedKey());

        if (keyBytes.length != 48) {
            throw new IllegalStateException("JWT encryption key must decode to exactly 48 bytes");
        }

        try {
            byte[] derivedKey = MessageDigest.getInstance("SHA-256").digest(keyBytes);
            return new SecretKeySpec(derivedKey, "AES");
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to initialize SHA-256 for key derivation", e);
            throw new AlgorithmNotFoundException("Required cryptographic algorithm not found", e);
        }
    }

    private Jws<Claims> extractSignClaims(String bearerToken) {
        return Jwts.parser().verifyWith(getSignKey())
            .build().parseSignedClaims(bearerToken);
    }

    private Jwe<Claims> extractEncryptedClaims(String bearerToken) {
        return Jwts.parser().decryptWith(getEncryptionKey())
            .build().parseEncryptedClaims(bearerToken);
    }

    public String extractEncryptedToken(String token) {
        Jws<Claims> jws = extractSignClaims(token);
        return extractClaimBody(jws, Claims::getSubject);
    }
}