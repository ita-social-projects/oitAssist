package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;

public class ForumResponderAccessRestrictedException extends AuthorizationException {
    public ForumResponderAccessRestrictedException(
        Long taskAssignmentId,
        Long userId) {
        super(
            ("User %s is not a forum responder for "
                + "task assignment %s").formatted(
                    userId,
                    taskAssignmentId),
            ErrorCode.FORUM_RESPONDER_ACCESS_RESTRICTED);
    }
}