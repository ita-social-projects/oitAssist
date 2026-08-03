package com.itasocialacademy.oitassist.security.dao.repository;

import com.itasocialacademy.oitassist.security.dao.model.UserTwoFactorAuth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTwoFactorAuthRepository extends JpaRepository<UserTwoFactorAuth, Long> {
    Optional<UserTwoFactorAuth> findByUserId(Long userId);
}