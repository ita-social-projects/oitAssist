package com.itasocialacademy.oitassist.security.jwt;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtHelper {
    public final static String ACCESS_TOKEN = "access";
    public final static String REFRESH_TOKEN = "refresh";

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

    private String generateToken(Map<String, Object> claims, String subject, Date expiryDate) {
        Date currentDate = new Date(System.currentTimeMillis());
        String encryptedToken = Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(currentDate)
            .expiration(expiryDate)
            .encryptWith(getSecretKey(jwtProperties.getEncryptedKey()), Jwts.ENC.A192CBC_HS384)
            .compact();
        return Jwts.builder()
            .subject(encryptedToken)
            .issuedAt(currentDate)
            .expiration(expiryDate)
            .signWith(getSecretKey(jwtProperties.getSignKey()), Jwts.SIG.HS384)
            .compact();
    }

    public String extractUsername(String token, String tokenType) {
        Jwe<Claims> jwe = extractEncryptedClaims(token);
        if (!tokenType.equals(jwe.getPayload().get("token_type"))) {
            throw new AuthenticationException("Invalid token type", ErrorCode.INVALID_TOKEN_TYPE);
        }
        return extractClaimBody(jwe, Claims::getSubject);
    }

    public <T> T extractClaimBody(ProtectedJwt<?, Claims> jwsClaims,
        Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(jwsClaims.getPayload());
    }

    private SecretKey getSecretKey(String key) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));
    }

    private Jws<Claims> extractSignClaims(String bearerToken) {
        return Jwts.parser().verifyWith(getSecretKey(jwtProperties.getSignKey()))
            .build().parseSignedClaims(bearerToken);
    }

    private Jwe<Claims> extractEncryptedClaims(String bearerToken) {
        return Jwts.parser().decryptWith(getSecretKey(jwtProperties.getEncryptedKey()))
            .build().parseEncryptedClaims(bearerToken);
    }

    public String extractEncryptedToken(String token) {
        Jws<Claims> jws = extractSignClaims(token);
        return extractClaimBody(jws, Claims::getSubject);
    }
}
