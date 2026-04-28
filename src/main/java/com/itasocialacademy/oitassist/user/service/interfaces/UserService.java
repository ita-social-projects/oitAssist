package com.itasocialacademy.oitassist.user.service.interfaces;

import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.NonNull;

public interface UserService extends BaseService<Long, CreateUserDTO, UpdateUserDTO, ResponseUserDTO> {
    UserDetailsImpl loadUserByUsername(String username);

    /**
     * Finds a user by email and returns their profile.
     *
     * @param email the user's email address
     * @return the user's profile DTO
     * @throws EntityNotFoundException if no user found with given email
     */
    @NonNull
    ResponseUserDTO loadUserByEmail(String email);

    /**
     * Finds a current authenticated user and returns their profile.
     *
     * @return the user's profile DTO
     * @throws EntityNotFoundException if no user found with given email
     */
    @NonNull
    ResponseUserDTO getCurrentUserProfile();
}
