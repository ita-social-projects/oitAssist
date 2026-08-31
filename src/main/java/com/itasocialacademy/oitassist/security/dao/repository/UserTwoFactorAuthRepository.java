package com.itasocialacademy.oitassist.security.dao.repository;

import com.itasocialacademy.oitassist.security.dao.model.UserTwoFactorAuth;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTwoFactorAuthRepository extends JpaRepository<UserTwoFactorAuth, Long> {
    Optional<UserTwoFactorAuth> findByUserId(Long userId);

    /**
     * Same lookup as {@link #findByUserId}, but takes a row-level write lock for
     * the transaction's duration. Required on any path that checks-then-writes
     * {@code lastUsedTotpBucket} (replay protection) — without this, two concurrent
     * requests submitting the same code (e.g. an intercepted code raced by an
     * attacker and the legitimate user) can both read pre-update state and both
     * pass the replay check before either commits.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM UserTwoFactorAuth t WHERE t.userId = :userId")
    Optional<UserTwoFactorAuth> findByUserIdForUpdate(@Param("userId") Long userId);
}