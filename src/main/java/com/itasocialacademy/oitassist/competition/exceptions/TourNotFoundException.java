package com.itasocialacademy.oitassist.competition.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class TourNotFoundException extends NotFoundException {

    public static final String ERROR_MESSAGE = "Tour with id: %d not found";

    public TourNotFoundException(Long id) {
        super(ERROR_MESSAGE.formatted(id), ErrorCode.ENTITY_NOT_FOUND);
    }
}
