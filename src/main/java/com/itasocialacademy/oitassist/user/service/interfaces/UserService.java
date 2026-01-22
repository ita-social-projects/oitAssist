package com.itasocialacademy.oitassist.user.service.interfaces;

import com.itasocialacademy.oitassist.user.dao.dto.UserVO;

public interface UserService {
    public UserVO getUserById(Long id);
}
