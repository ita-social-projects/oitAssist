package com.itasocialacademy.oitassist.competition.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class StageNotFoundException extends NotFoundException {
    private static final String ERROR_MESSAGE = "Stage with id: %d not found";

    public StageNotFoundException(Long id) {
        super(ERROR_MESSAGE.formatted(id), ErrorCode.ENTITY_NOT_FOUND);
    }
}
