package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.core.rest.service.AbstractServiceImpl;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserServiceImpl
    extends AbstractServiceImpl<Long, User, CreateUserDTO, UpdateUserDTO, ResponseUserDTO, UserRepository, UserMapper>
    implements UserService {
    private final SecurityFacade securityFacade;

    protected UserServiceImpl(UserRepository repository, UserMapper mapper, SecurityFacade securityFacade) {
        super(repository, mapper);
        this.securityFacade = securityFacade;
    }

    public UserDetailsImpl loadUserByUsername(@NonNull String username) {
        Optional<User> user = repository.findUserByEmail(username);
        return user.map(mapper::toUserDetails).orElse(null);
    }

    @Override
    @NonNull
    public ResponseUserDTO loadUserByEmail(@NonNull String email) {
        Optional<User> user = repository.findUserByEmail(email);

        return user.map(mapper::toResponseUserDTO)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
    }

    @Override
    @NonNull
    public ResponseUserDTO getCurrentUserProfile() {
        String email = securityFacade.getCurrentUserEmail()
            .orElseThrow(() -> new EntityNotFoundException("No authenticated user"));

        return loadUserByEmail(email);
    }
}
