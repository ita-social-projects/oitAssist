package com.itasocialacademy.oitassist.competition.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class CompetitionNotFoundException extends NotFoundException {
    public CompetitionNotFoundException(String message) {
        super(message, ErrorCode.ENTITY_NOT_FOUND);
    }
}
