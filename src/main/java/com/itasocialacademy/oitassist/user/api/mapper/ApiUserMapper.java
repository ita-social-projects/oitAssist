package com.itasocialacademy.oitassist.user.api.mapper;

import com.itasocialacademy.oitassist.user.api.dto.CurrentUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApiUserMapper {
    CurrentUserDTO toCurrentUserDTO(ResponseUserDTO responseUserDTO);
}
