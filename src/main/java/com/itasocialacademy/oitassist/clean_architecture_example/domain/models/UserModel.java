package com.itasocialacademy.oitassist.clean_architecture_example.domain.models;

public record UserModel(
    String username,
    String password,
    String email
) {
}
