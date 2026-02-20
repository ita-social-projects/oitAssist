package com.itasocialacademy.oitassist.user.dao.repository;

import com.itasocialacademy.oitassist.user.dao.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findUserById(Long id);

    Optional<User> findUserByEmail(String email);
}