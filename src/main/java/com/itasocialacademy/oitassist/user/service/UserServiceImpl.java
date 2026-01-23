package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.user.dao.dto.UserVO;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.mapper.UserVoMapper;
import com.itasocialacademy.oitassist.user.mapper.request.CreateUserRequestMapper;
import com.itasocialacademy.oitassist.user.mapper.request.UpdateUserRequestMapper;
import com.itasocialacademy.oitassist.user.mapper.response.CreateUserResponseMapper;
import com.itasocialacademy.oitassist.user.mapper.response.UpdateUserResponseMapper;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final CreateUserRequestMapper createUserRequestMapper;
    private final CreateUserResponseMapper createUserResponseMapper;
    private final UpdateUserRequestMapper updateUserRequestMapper;
    private final UpdateUserResponseMapper updateUserResponseMapper;
    private final UserVoMapper userVoMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserVO getUserById(Long id) {
        User user = userRepository.findUserById(id);
        return userVoMapper.toDto(user);
    }
}
