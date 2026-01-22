package com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.persistance.repository;

import com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.persistance.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;

public interface ClearUserRepository extends CrudRepository<UserEntity, Long> {
}
