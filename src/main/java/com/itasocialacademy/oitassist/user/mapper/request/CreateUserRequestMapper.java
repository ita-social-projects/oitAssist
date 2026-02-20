package com.itasocialacademy.oitassist.user.mapper.request;

import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserRequest;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.time.Instant;

@Mapper(componentModel = "spring", imports = {Instant.class})
public interface CreateUserRequestMapper {
    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "userStatus", constant = "NOT_ACTIVATED")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(source = "lastName", target = "surname")
    User toEntity(CreateUserRequest request);
}
