package com.itasocialacademy.oitassist.auth.mapper;

import com.itasocialacademy.oitassist.auth.dto.request.RegisterRequest;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.time.Instant;

@Mapper(componentModel = "spring", imports = {Instant.class})
public interface RegisterRequestMapper {
    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "userStatus", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(source = "lastName", target = "surname")
    User toEntity(RegisterRequest request);
}
