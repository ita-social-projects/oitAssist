package com.itasocialacademy.oitassist.security.dao.repository;

import com.itasocialacademy.oitassist.security.dao.model.UserRecoveryCode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRecoveryCodeRepository extends JpaRepository<UserRecoveryCode, Long> {
    List<UserRecoveryCode> findByTwoFactorAuthIdAndUsedFalse(Long twoFactorAuthId);

    void deleteByTwoFactorAuthId(Long twoFactorAuthId);
}