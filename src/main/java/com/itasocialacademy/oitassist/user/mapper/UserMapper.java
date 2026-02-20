package com.itasocialacademy.oitassist.user.mapper;

import com.itasocialacademy.oitassist.core.rest.mapper.GeneralMapper;
import com.itasocialacademy.oitassist.user.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper extends GeneralMapper<User, CreateUserDTO, UpdateUserDTO, ResponseUserDTO> {
    UserDetailsImpl toUserDetails(User entity);
}
