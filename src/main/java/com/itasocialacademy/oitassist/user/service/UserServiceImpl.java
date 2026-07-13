package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.exceptions.AdminRoleModificationException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import com.itasocialacademy.oitassist.user.exceptions.UserRoleSelfChangeException;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final SecurityFacade securityFacade;
    private final UserRepository repository;
    private final UserMapper mapper;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Looks up the user by email and maps to {@link UserAuthDetails} via
     * {@link UserMapper#toUserAuthDetails}. No {@code security}-owned types are
     * involved in this path.
     * </p>
     */
    @Override
    public Optional<UserAuthDetails> findAuthDetailsByEmail(String email) {
        return repository.findUserByEmail(email)
            .map(mapper::toUserAuthDetails);
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
            .orElseThrow(UserNotFoundException::new);
    }

    @Override
    @NonNull
    public ResponseUserDTO getCurrentUserProfile() {
        String email = securityFacade.getCurrentUserEmail()
            .orElseThrow(() -> new AuthorizationException("User is not authenticated", ErrorCode.ACCESS_DENIED));

        return loadUserByEmail(email);
    }

    @Override
    @NonNull
    public ResponseUserDTO changeUserRole(@NonNull Long userId, @NonNull Role newRole) {
        if (securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User is not authenticated", ErrorCode.ACCESS_DENIED))
            .equals(userId)) {
            throw new UserRoleSelfChangeException();
        }
        if (!securityFacade.hasRole(String.valueOf(Role.ADMIN))) {
            throw new InsufficientPermissionsException();
        }
        User user = repository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
        if (user.getRole().equals(Role.ADMIN)) {
            throw new AdminRoleModificationException();
        }
        user.setRole(newRole);
        return mapper.toResponseUserDTO(repository.save(user));
    }

    @Override
    public @NonNull Page<ResponseUserDTO> getUsers(@NonNull Pageable pageable, String search) {
        if (!securityFacade.hasRole(String.valueOf(Role.ADMIN))) {
            throw new InsufficientPermissionsException();
        }
        if (search != null && !search.isBlank()) {
            String normalizedSearch = search.trim()
                .replaceAll("\\s+", " ")
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
            return repository.findAllBySearch(normalizedSearch, pageable).map(mapper::toResponseUserDTO);
        } else {
            return repository.findAll(pageable).map(mapper::toResponseUserDTO);
        }
    }
}
