package com.itasocialacademy.oitassist.chat.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;

public class ForumResponderActiveReviewException extends BusinessException {
    public ForumResponderActiveReviewException(
        Long taskAssignmentId,
        Long responderUserId) {
        super(
            ("Forum responder %s cannot be revoked from "
                + "task assignment %s because the responder "
                + "owns an active open question review").formatted(
                    responderUserId,
                    taskAssignmentId),
            ErrorCode.FORUM_RESPONDER_ACTIVE_REVIEW_CONFLICT);
    }
}