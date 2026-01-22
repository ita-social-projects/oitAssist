package com.itasocialacademy.oitassist.clean_architecture_example.application.service;

import com.itasocialacademy.oitassist.clean_architecture_example.application.usecase.CreateUser;
import com.itasocialacademy.oitassist.clean_architecture_example.domain.dto.response.CreateUserResponse;
import com.itasocialacademy.oitassist.clean_architecture_example.domain.models.UserModel;
import com.itasocialacademy.oitassist.core.exception.UserValidationException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserService {
    private final CreateUser createUser;
    private final UserObjectValidation userValidationService;

    public CreateUserResponse create (UserModel user) {
        String validationMessage = userValidationService.validateUserModel(user);

        if (validationMessage != null)
            throw new UserValidationException(validationMessage);

        UserModel modelObj = createUser.execute(user);

        return new CreateUserResponse(modelObj.username(), modelObj.email());
    }
}
