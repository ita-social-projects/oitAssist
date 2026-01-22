package com.itasocialacademy.oitassist.clean_architecture_example.application.gateways;

import com.itasocialacademy.oitassist.clean_architecture_example.domain.models.UserModel;

public interface UserGateway {
    public UserModel create (UserModel user);
}
