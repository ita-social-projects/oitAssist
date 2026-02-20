package com.itasocialacademy.oitassist.user.dao.repository;

import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends EntityRepository<User, Long> {
    Optional<User> findUserByEmail(String email);
}