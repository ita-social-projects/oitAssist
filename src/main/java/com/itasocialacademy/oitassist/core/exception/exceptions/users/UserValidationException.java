package com.itasocialacademy.oitassist.core.exception.exceptions.users;

import org.springframework.modulith.NamedInterface;

@NamedInterface("UserValidationException")
public class UserValidationException extends RuntimeException{
    public UserValidationException(String message) {
        super(message);
    }
}
