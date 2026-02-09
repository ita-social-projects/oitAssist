package com.itasocialacademy.oitassist.user.dao.repository;

import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends EntityRepository<User, Long> {
    User findUserByEmail(String email);
}