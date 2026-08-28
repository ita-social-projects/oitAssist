package com.itasocialacademy.oitassist.submission.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class TourIsNotInProgressException extends BusinessException {
    public TourIsNotInProgressException() {
        super("Can not submit the task when tour is not in progress", ErrorCode.TOUR_NOT_IN_PROGRESS);
    }
}
