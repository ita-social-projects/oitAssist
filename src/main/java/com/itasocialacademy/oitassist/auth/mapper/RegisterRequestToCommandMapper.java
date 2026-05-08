package com.itasocialacademy.oitassist.auth.mapper;

import com.itasocialacademy.oitassist.auth.dto.request.RegisterRequest;
import com.itasocialacademy.oitassist.user.api.dto.RegisterCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegisterRequestToCommandMapper {
    @Mapping(source = "lastName", target = "surname")
    RegisterCommand toCommand(RegisterRequest request);
}
