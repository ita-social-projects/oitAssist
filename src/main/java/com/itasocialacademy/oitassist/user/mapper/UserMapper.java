package com.itasocialacademy.oitassist.user.mapper;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "authorities", expression = "java(mapAuthorities(entity))")
    UserDetailsImpl toUserDetails(User entity);

    ResponseUserDTO toResponseUserDTO(User entity);

    /**
     * Maps a {@link User} entity to the cross-module auth projection
     * {@link UserAuthDetails}.
     *
     * <p>
     * Fields {@code id}, {@code email}, {@code password}, and {@code role} share
     * the same names on both sides — MapStruct resolves them automatically with no
     * explicit {@code @Mapping} annotations required.
     * </p>
     *
     * <p>
     * Called by
     * {@link com.itasocialacademy.oitassist.user.service.interfaces.UserService#findAuthDetailsByEmail}
     * which is in turn called by
     * {@link com.itasocialacademy.oitassist.user.api.facade.UserFacadeImpl#findByEmail}.
     * </p>
     */
    UserAuthDetails toUserAuthDetails(User entity);

    default List<SimpleGrantedAuthority> mapAuthorities(User entity) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + entity.getRole().name()));
    }
}
