package com.itasocialacademy.oitassist.user.mapper;

import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.model.ProfileUpdateRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileUpdateRequestMapper {
    ResponseProfileUpdateRequestDTO toResponseProfileUpdateRequestDTO(ProfileUpdateRequest profileUpdateRequest);
}
