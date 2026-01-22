package com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.config;

import com.itasocialacademy.oitassist.clean_architecture_example.application.gateways.UserGateway;
import com.itasocialacademy.oitassist.clean_architecture_example.application.service.UserObjectValidation;
import com.itasocialacademy.oitassist.clean_architecture_example.application.service.UserService;
import com.itasocialacademy.oitassist.clean_architecture_example.application.usecase.CreateUser;
import com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.gateway.UserRepositoryGateway;
import com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.persistance.repository.ClearUserRepository;
import com.itasocialacademy.oitassist.clean_architecture_example.presentation.mapper.UserEntityMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDependencyInjection {

    @Bean
    CreateUser createUser(UserGateway userGateway) {
        return new CreateUser(userGateway);
    }

    @Bean
    UserGateway userGateway(ClearUserRepository userRepository, UserEntityMapper userMapper) {
        return new UserRepositoryGateway(userRepository, userMapper);
    }

    @Bean
    UserObjectValidation userValidationService() {
        return new UserObjectValidation();
    }

    @Bean
    UserService userService(CreateUser createUser, UserObjectValidation userValidationService) {
        return new UserService(createUser, userValidationService);
    }
}
