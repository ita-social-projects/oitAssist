package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;

public class InvalidForumResponderCandidateException extends ValidationException {
    public InvalidForumResponderCandidateException(Long userId, Role actualRole, UserStatus actualStatus) {
        super(
            ("User %s cannot be assigned as a forum responder: "
                + "required role is ORG and required status "
                + "is ACTIVE; actual role=%s, status=%s").formatted(
                    userId,
                    actualRole,
                    actualStatus),
            ErrorCode.FORUM_RESPONDER_INVALID);
    }
}