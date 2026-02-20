package com.itasocialacademy.oitassist.user.dao.repository;

import com.itasocialacademy.oitassist.user.dao.model.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, Long> {
}
