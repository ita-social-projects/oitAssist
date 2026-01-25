package com.itasocialacademy.oitassist.user.api.facade;

import com.itasocialacademy.oitassist.user.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.api.dto.CreateUserCommand;
import com.itasocialacademy.oitassist.user.api.dto.UserDto;
import com.itasocialacademy.oitassist.user.api.dto.UserPublicDto;
import java.util.Optional;
import java.util.UUID;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {
    // private final ClearUserRepository userRepository
    private final UserService userService;

    @Override
    public UserDto createUser(CreateUserCommand command) {
        return null;
    }

    @Override
    public UserDto getUserById(UUID userId) {
        return null;
    }

    @Override
    public boolean existsByEmail(String email) {
        return false;
    }

    @Override
    public UserDetailsImpl getUserByEmail(String email) {
        return userService.loadUserByUsername(email);
    }

    @Override
    public Optional<UserPublicDto> getPublicUser(UUID userId) {
        return Optional.empty();
    }

    @Override
    public void disableUser(UUID userId) {
    }
}
