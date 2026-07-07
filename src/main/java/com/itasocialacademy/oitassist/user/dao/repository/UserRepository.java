package com.itasocialacademy.oitassist.user.dao.repository;

import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@NamedInterface("UserRepository")
public interface UserRepository extends EntityRepository<User, Long> {
    Optional<User> findUserByEmail(String email);

    @Query("""
            SELECT u
            FROM User u
            WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.surname) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.middleName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(CONCAT(u.firstName, ' ', u.surname)) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(CONCAT(u.surname, ' ', u.firstName)) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(CONCAT(u.firstName, ' ', u.middleName)) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(CONCAT(u.firstName, ' ', u.surname, ' ', u.middleName)) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(CONCAT(u.surname, ' ', u.firstName, ' ', u.middleName)) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Page<User> findAllBySearch(String search, Pageable pageable);
}