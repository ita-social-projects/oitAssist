package com.itasocialacademy.oitassist.clean_architecture_example.application.service;

import com.itasocialacademy.oitassist.clean_architecture_example.domain.dto.request.CreateUserRequest;
import com.itasocialacademy.oitassist.clean_architecture_example.domain.models.UserModel;
import com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.persistance.entity.UserEntity;

public class UserObjectValidation {
    public String validateUserEntity(UserEntity userEntity) {
        if (userEntity.getUsername() == null || userEntity.getUsername().isEmpty())
            return "Username is required";

        else if (userEntity.getPassword() == null || userEntity.getPassword().isEmpty())
            return "Password is required";

        else if (userEntity.getEmail() == null || userEntity.getEmail().isEmpty())
            return "Email is required";

        return null;
    }

    public String validateUserModel(UserModel userModel) {
        if (userModel.username() == null || userModel.username().isEmpty())
            return "Username is required";

        else if (userModel.password() == null || userModel.password().isEmpty())
            return "Password is required";

        else if (userModel.email() == null || userModel.email().isEmpty())
            return "Email is required";

        return null;
    }

    public String validateCreateUserRequest(CreateUserRequest createUserRequest) {
        if (createUserRequest.getUsername() == null || createUserRequest.getUsername().isEmpty())
            return "Username is required";

        else if (createUserRequest.getPassword() == null || createUserRequest.getPassword().isEmpty())
            return "Password is required";

        else if (createUserRequest.getEmail() == null || createUserRequest.getEmail().isEmpty())
            return "Email is required";

        return null;
    }
}
