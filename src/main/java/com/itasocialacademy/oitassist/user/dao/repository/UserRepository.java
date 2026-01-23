package com.itasocialacademy.oitassist.user.dao.repository;

import com.itasocialacademy.oitassist.user.dao.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findUserById(Long id);
}