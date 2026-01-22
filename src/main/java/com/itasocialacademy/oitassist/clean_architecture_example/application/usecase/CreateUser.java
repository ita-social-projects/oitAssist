package com.itasocialacademy.oitassist.clean_architecture_example.application.usecase;

import com.itasocialacademy.oitassist.clean_architecture_example.application.gateways.UserGateway;
import com.itasocialacademy.oitassist.clean_architecture_example.domain.models.UserModel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateUser {
    private final UserGateway userGateway;

    public UserModel execute (UserModel user) {
        return userGateway.create(user);
    }
}
