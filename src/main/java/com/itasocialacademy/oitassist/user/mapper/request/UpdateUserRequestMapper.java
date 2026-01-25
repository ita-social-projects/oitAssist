package com.itasocialacademy.oitassist.user.mapper.request;

import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserRequest;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UpdateUserRequestMapper {
    User toEntity(UpdateUserRequest request);

    UpdateUserRequest toDto(User user);
}
