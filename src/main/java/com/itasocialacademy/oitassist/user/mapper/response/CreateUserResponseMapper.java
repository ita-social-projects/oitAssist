package com.itasocialacademy.oitassist.user.mapper.response;

import com.itasocialacademy.oitassist.user.dao.dto.response.CreateUserResponse;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CreateUserResponseMapper {
    User toEntity(CreateUserResponse response);

    CreateUserResponse toDto(User user);
}
