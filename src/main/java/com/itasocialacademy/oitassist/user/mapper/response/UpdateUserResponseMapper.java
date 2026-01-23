package com.itasocialacademy.oitassist.user.mapper.response;

import com.itasocialacademy.oitassist.user.dao.dto.response.UpdateUserResponse;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UpdateUserResponseMapper {
    User toEntity(UpdateUserResponse response);

    UpdateUserResponse toDto(User user);
}
