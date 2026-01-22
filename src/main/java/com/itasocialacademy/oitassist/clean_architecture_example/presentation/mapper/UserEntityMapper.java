package com.itasocialacademy.oitassist.clean_architecture_example.presentation.mapper;

import com.itasocialacademy.oitassist.clean_architecture_example.domain.models.UserModel;
import com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.persistance.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {
    UserEntity toEntity (UserModel domainObject);
    UserModel toDomain (UserEntity entityObject);
}
