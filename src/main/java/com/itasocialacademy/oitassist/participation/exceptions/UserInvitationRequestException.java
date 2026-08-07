package com.itasocialacademy.oitassist.participation.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class UserInvitationRequestException extends BusinessException {
    public UserInvitationRequestException(String message) {
        super(message, ErrorCode.USER_PARTICIPATION_REQUEST_RESTRICTED);
    }
}
