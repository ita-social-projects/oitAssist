package com.itasocialacademy.oitassist.auth.dao.repository;

import com.itasocialacademy.oitassist.user.dao.model.UserActivationToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivationTokenRepository extends JpaRepository<UserActivationToken, Long> {
    Optional<UserActivationToken> findByToken(String token);
}
