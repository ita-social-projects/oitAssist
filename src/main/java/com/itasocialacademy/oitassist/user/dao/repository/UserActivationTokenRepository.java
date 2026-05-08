package com.itasocialacademy.oitassist.user.dao.repository;

import com.itasocialacademy.oitassist.user.dao.model.UserActivationToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link UserActivationToken} entities.
 *
 * <p>
 * Moved from {@code auth.dao.repository} to {@code user.dao.repository} as part
 * of consolidating the activation lifecycle entirely within the {@code user}
 * module. {@link UserActivationToken} is an aggregate owned by {@code user};
 * its repository belongs in the same module.
 * </p>
 */
@Repository
public interface UserActivationTokenRepository extends JpaRepository<UserActivationToken, Long> {
    Optional<UserActivationToken> findByToken(String token);
}