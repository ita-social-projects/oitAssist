package com.itasocialacademy.oitassist.security.dao.model;

import com.itasocialacademy.oitassist.security.dao.enums.TwoFactorMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks a user's two-factor authentication enrollment and verification state.
 *
 * <p>
 * Owned entirely by the {@code security} module. Deliberately holds no
 * reference to {@code user.dao.model.User} — {@code security}'s
 * {@code package-info.java} only allows depending on {@code core}, so only the
 * raw {@code userId} is stored here. Callers that need the owning user go
 * through {@code SecurityUserProvider}.
 * </p>
 *
 * <p>
 * Absence of a row for a given {@code userId} means the user has never
 * enrolled. {@code enabled = false} means enrollment was started but the
 * confirmation step (the user proving they can produce a valid code) has not
 * yet succeeded — see {@link #confirmEnabled()}.
 * </p>
 *
 * <p>
 * No {@code @ToString} or {@code @EqualsAndHashCode} override is provided,
 * mirroring {@code UserActivationToken} rather than {@code User}: this entity
 * carries secrets ({@code totpSecret}, {@code pendingEmailOtpHash}), so the
 * default identity-based {@link Object} behavior — which never prints field
 * values — is preferred over a generated {@code toString()} that could leak
 * them into logs.
 * </p>
 */
@Entity
@Table(name = "user_two_factor_auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTwoFactorAuth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 50)
    private TwoFactorMethod method;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Column(name = "last_used_totp_bucket")
    private Long lastUsedTotpBucket;

    @Column(name = "pending_email_otp_hash", length = 72)
    private String pendingEmailOtpHash;

    @Column(name = "pending_email_otp_expires_at")
    private Instant pendingEmailOtpExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Starts a new enrollment. {@code enabled} stays false until
     * {@link #confirmEnabled()} succeeds, so a failed/abandoned scan never leaves
     * the account silently protected by a secret the user never actually confirmed.
     */
    public static UserTwoFactorAuth startEnrollment(Long userId, TwoFactorMethod method, String totpSecret) {
        Instant now = Instant.now();
        return UserTwoFactorAuth.builder()
            .userId(userId)
            .method(method)
            .enabled(false)
            .totpSecret(totpSecret)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    public void confirmEnabled() {
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    public void disable() {
        this.enabled = false;
        this.totpSecret = null;
        clearPendingEmailOtp();
        this.updatedAt = Instant.now();
    }

    /**
     * Records that a TOTP time-bucket was just consumed, for replay-window
     * protection (plan section 2.4b): a code must not validate twice within the
     * accepted clock-drift tolerance.
     */
    public void recordTotpUse(long timeBucket) {
        this.lastUsedTotpBucket = timeBucket;
        this.updatedAt = Instant.now();
    }

    public boolean isTotpBucketAlreadyUsed(long timeBucket) {
        return lastUsedTotpBucket != null && timeBucket <= lastUsedTotpBucket;
    }

    public void setPendingEmailOtp(String hash, Instant expiresAt) {
        this.pendingEmailOtpHash = hash;
        this.pendingEmailOtpExpiresAt = expiresAt;
        this.updatedAt = Instant.now();
    }

    public void clearPendingEmailOtp() {
        this.pendingEmailOtpHash = null;
        this.pendingEmailOtpExpiresAt = null;
        this.updatedAt = Instant.now();
    }

    public boolean isPendingEmailOtpExpired() {
        return pendingEmailOtpExpiresAt == null || pendingEmailOtpExpiresAt.isBefore(Instant.now());
    }
}