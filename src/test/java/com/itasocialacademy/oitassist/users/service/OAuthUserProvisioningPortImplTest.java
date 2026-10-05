package com.itasocialacademy.oitassist.users.service;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.api.dto.OAuthProvisionCommand;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.service.OAuthUserProvisioningPortImpl;
import com.itasocialacademy.oitassist.user.service.interfaces.RegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit test for OAuthUserProvisioningPortImpl")
class OAuthUserProvisioningPortImplTest {
    @Mock
    private RegistrationService registrationService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private OAuthUserProvisioningPortImpl provisioningPort;

    @Test
    @DisplayName(
        "provisionOAuthUser should build command, delegate to registration service and return mapped user details")
    void provisionOAuthUser_ShouldReturnMappedUserDetails_WhenUserIsProvisioned() {
        String email = "ivan@example.com";
        String firstName = "Ivan";
        String surname = "Petrenko";
        String middleName = "Ivanovych";
        String phoneNumber = "+380501234567";

        OAuthProvisionCommand expectedCommand = OAuthProvisionCommand.builder()
            .email(email)
            .firstName(firstName)
            .surname(surname)
            .middleName(middleName)
            .phoneNumber(phoneNumber)
            .build();

        User user = User.builder()
            .email(email)
            .build();

        UserDetailsImpl expected = UserDetailsImpl.builder()
            .email(email)
            .build();

        when(registrationService.provisionOAuthUser(expectedCommand)).thenReturn(user);
        when(userMapper.toUserDetails(user)).thenReturn(expected);

        UserDetailsImpl result = provisioningPort.provisionOAuthUser(
            email, firstName, surname, middleName, phoneNumber);

        assertThat(result).isSameAs(expected);

        verify(registrationService).provisionOAuthUser(expectedCommand);
        verify(userMapper).toUserDetails(user);
    }
}