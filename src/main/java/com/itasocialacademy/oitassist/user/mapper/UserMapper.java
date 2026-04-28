package com.itasocialacademy.oitassist.user.mapper;

import com.itasocialacademy.oitassist.core.rest.mapper.GeneralMapper;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper extends GeneralMapper<User, CreateUserDTO, UpdateUserDTO, ResponseUserDTO> {
    @Mapping(target = "authorities", expression = "java(mapAuthorities(entity))")
    UserDetailsImpl toUserDetails(User entity);

    ResponseUserDTO toResponseUserDTO(User entity);

    default List<SimpleGrantedAuthority> mapAuthorities(User entity) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + entity.getRole().name()));
    }
}
