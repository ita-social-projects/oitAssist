package com.itasocialacademy.oitassist.user.mapper;

import com.itasocialacademy.oitassist.user.dao.dto.UserVO;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserVoMapper {
    User toEntity (UserVO vo);
    UserVO toDto (User user);
}
