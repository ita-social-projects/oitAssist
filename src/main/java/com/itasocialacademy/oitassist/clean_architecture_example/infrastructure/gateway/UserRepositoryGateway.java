package com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.gateway;

import com.itasocialacademy.oitassist.clean_architecture_example.application.gateways.UserGateway;
import com.itasocialacademy.oitassist.clean_architecture_example.domain.models.UserModel;
import com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.persistance.entity.UserEntity;
import com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.persistance.repository.ClearUserRepository;
import com.itasocialacademy.oitassist.clean_architecture_example.presentation.mapper.UserEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class UserRepositoryGateway implements UserGateway {
    private final ClearUserRepository userRepository;
    private final UserEntityMapper userMapper;

    @Override
    @Transactional
    public UserModel create(UserModel user) {
        UserEntity userEntity = userMapper.toEntity(user);
        UserEntity saved = userRepository.save(userEntity);

        return userMapper.toDomain(saved);
    }
}
