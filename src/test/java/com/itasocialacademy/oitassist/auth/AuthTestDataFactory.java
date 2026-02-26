package com.itasocialacademy.oitassist.auth;

import com.itasocialacademy.oitassist.auth.dao.dto.request.RegisterRequest;
import com.itasocialacademy.oitassist.auth.dao.dto.request.ResendVerificationMailRequest;

public class AuthTestDataFactory {
    public final static String REGISTRATION_PATH = "/api/v1/registration";
    public final static String ACTIVATION_RESEND_PATH = "/api/v1/user-activation/resend";
    private final static String FIRST_NAME = "First name";
    private final static String LAST_NAME = "Last name";
    private final static String MIDDLE_NAME = "Middle name";
    private final static String PHONE_NUMBER = "+380991234567";
    private final static String PASSWORD = "password123";
    public final static String EMAIL = "test@test.com";

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
