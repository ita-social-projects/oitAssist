package com.itasocialacademy.oitassist.submission.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class NotAParticipantException extends BusinessException {
    public NotAParticipantException() {
        super("Can not submit the task when you are not a tour participant", ErrorCode.NOT_A_PARTICIPANT);
    }
}
