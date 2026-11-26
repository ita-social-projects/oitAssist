package com.itasocialacademy.oitassist.school.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class SchoolNotFoundException extends NotFoundException {
    private static final String ERROR_MESSAGE = "School with id: %d not found";

    public SchoolNotFoundException(Long id) {
        super(ERROR_MESSAGE.formatted(id), ErrorCode.ENTITY_NOT_FOUND);
    }
}
