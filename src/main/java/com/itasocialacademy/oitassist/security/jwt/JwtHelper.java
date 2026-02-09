package com.itasocialacademy.oitassist.security.jwt;

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
    private final JwtProperties jwtProperties;

    public JwtHelper(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createToken(Map<String, Object> claims, String subject) {
        Date expiryDate = Date.from(Instant.ofEpochMilli(System.currentTimeMillis() + jwtProperties.getValidity()));
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

    public String extractUsername(String token) {
        Jwe<Claims> jwe = extractEncryptedClaims(token);
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
