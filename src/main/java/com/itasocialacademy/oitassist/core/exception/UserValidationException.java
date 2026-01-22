package com.itasocialacademy.oitassist.core.exception;

import org.springframework.modulith.NamedInterface;

@NamedInterface("UserValidationException")
public class UserValidationException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public UserValidationException(String message) {
        super(message);
    }
}
