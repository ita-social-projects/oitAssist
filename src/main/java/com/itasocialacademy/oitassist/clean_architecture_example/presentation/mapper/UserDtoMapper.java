package com.itasocialacademy.oitassist.clean_architecture_example.presentation.mapper;

import com.itasocialacademy.oitassist.clean_architecture_example.domain.dto.request.CreateUserRequest;
import com.itasocialacademy.oitassist.clean_architecture_example.domain.dto.response.CreateUserResponse;
import com.itasocialacademy.oitassist.clean_architecture_example.domain.models.UserModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    CreateUserResponse toResponse(UserModel domainObject);
    UserModel toDomain (CreateUserRequest request);
}
