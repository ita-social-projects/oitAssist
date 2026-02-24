package com.itasocialacademy.oitassist.user.dao.model;

import com.itasocialacademy.oitassist.user.exceptions.ActivationTokenSendingTimeoutException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_activation_tokens")
public class UserActivationToken {
    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public void validateResendAllowed(Duration timeout) {
        Instant nextAllowed = lastSentAt.plus(timeout);
        if (nextAllowed.isAfter(Instant.now())) {
            throw new ActivationTokenSendingTimeoutException(nextAllowed);
        }
    }

    public void markSent() {
        lastSentAt = Instant.now();
    }

    public static UserActivationToken generateActivationToken() {
        return UserActivationToken.builder().createdAt(Instant.now())
            .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES)).token(UUID.randomUUID().toString())
            .lastSentAt(Instant.now()).build();
    }
}
