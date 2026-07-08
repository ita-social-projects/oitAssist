package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.api.interfaces.OAuthUserProvisioningPort;
import com.itasocialacademy.oitassist.user.api.dto.OAuthProvisionCommand;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.service.interfaces.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implements {@link OAuthUserProvisioningPort}, the contract defined by
 * {@code security} (Dependency Inversion — same pattern as
 * {@code SecurityUserProviderImpl}).
 *
 * <p>
 * Translates the port's primitive parameters into the existing
 * {@link OAuthProvisionCommand}, delegates the find-or-create/auto-activate
 * logic to {@link RegistrationService#provisionOAuthUser}, and maps the
 * resulting {@link User} entity to {@link UserDetailsImpl} via the existing
 * {@link UserMapper#toUserDetails}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OAuthUserProvisioningPortImpl implements OAuthUserProvisioningPort {
    private final RegistrationService registrationService;
    private final UserMapper userMapper;

    @Override
    public UserDetailsImpl provisionOAuthUser(String email, String firstName, String surname,
        String middleName, String phoneNumber) {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(email)
            .firstName(firstName)
            .surname(surname)
            .middleName(middleName)
            .phoneNumber(phoneNumber)
            .build();

        User user = registrationService.provisionOAuthUser(command);
        return userMapper.toUserDetails(user);
    }
}