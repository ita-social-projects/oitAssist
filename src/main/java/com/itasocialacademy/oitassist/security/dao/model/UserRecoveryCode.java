package com.itasocialacademy.oitassist.security.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A single one-time backup code, issued in a batch of ten at enrollment.
 *
 * <p>
 * Deliberately holds a flat {@code twoFactorAuthId} rather than a
 * {@code @ManyToOne} object reference to {@link UserTwoFactorAuth}. Both
 * entities live in the {@code security} module, so a real relationship is
 * technically possible — but recovery-code verification sits on the login hot
 * path, and every access here is already scoped by {@code two_factor_auth_id}
 * through the repository. A flat id avoids any risk of an accidental lazy/eager
 * collection load on a path that runs on every login.
 * </p>
 *
 * <p>
 * No {@code @ToString}/{@code @EqualsAndHashCode} override, for the same reason
 * as {@link UserTwoFactorAuth}: {@code codeHash} is a secret.
 * </p>
 */
@Entity
@Table(name = "user_recovery_code")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRecoveryCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "two_factor_auth_id", nullable = false)
    private Long twoFactorAuthId;

    @Column(name = "code_hash", nullable = false, length = 72)
    private String codeHash;

    @Column(name = "used", nullable = false)
    private boolean used;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static UserRecoveryCode issue(Long twoFactorAuthId, String codeHash) {
        return UserRecoveryCode.builder()
            .twoFactorAuthId(twoFactorAuthId)
            .codeHash(codeHash)
            .used(false)
            .createdAt(Instant.now())
            .build();
    }

    public void markUsed() {
        this.used = true;
        this.usedAt = Instant.now();
    }
}