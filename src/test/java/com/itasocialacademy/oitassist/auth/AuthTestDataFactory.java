package com.itasocialacademy.oitassist.auth;

import com.itasocialacademy.oitassist.auth.dto.request.RegisterRequest;
import com.itasocialacademy.oitassist.auth.dto.request.ResendVerificationMailRequest;

public class AuthTestDataFactory {
    public static final String REGISTRATION_PATH = "/api/v1/registration";
    public static final String ACTIVATION_RESEND_PATH = "/api/v1/user-activation/resend";
    private static final String FIRST_NAME = "First name";
    private static final String LAST_NAME = "Last name";
    private static final String MIDDLE_NAME = "Middle name";
    private static final String PHONE_NUMBER = "+380991234567";
    private static final String PASSWORD = "password123";
    public static final String EMAIL = "test@test.com";

    public static RegisterRequest validRegisterRequest() {
        return RegisterRequest.builder()
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .middleName(MIDDLE_NAME)
            .phoneNumber(PHONE_NUMBER)
            .password(PASSWORD)
            .email(EMAIL)
            .build();
    }

    public static RegisterRequest invalidRegisterRequest() {
        return RegisterRequest.builder()
            .firstName(FIRST_NAME)
            .email(null)
            .build();
    }

    public static ResendVerificationMailRequest validResendVerificationMailRequest() {
        return ResendVerificationMailRequest.builder()
            .email(EMAIL)
            .build();
    }

    public static ResendVerificationMailRequest invalidResendVerificationMailRequest() {
        return ResendVerificationMailRequest.builder().build();
    }
}
