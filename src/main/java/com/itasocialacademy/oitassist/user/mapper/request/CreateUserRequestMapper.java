package com.itasocialacademy.oitassist.user.mapper.request;

import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserRequest;
import com.itasocialacademy.oitassist.user.dao.model.User;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CreateUserRequestMapper {
    User toEntity (CreateUserRequest request);
    CreateUserRequest toDto (User user);
}
