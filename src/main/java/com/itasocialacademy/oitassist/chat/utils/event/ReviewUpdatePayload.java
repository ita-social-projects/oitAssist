package com.itasocialacademy.oitassist.chat.utils.event;

import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
import java.util.Objects;

/**
 * Contains the current question snapshot for the assigned administrator's
 * review projection.
 */
public record ReviewUpdatePayload(
    AdminQuestionInboxItemResponseDTO question)
    implements RealtimePayload {
    public ReviewUpdatePayload {
        Objects.requireNonNull(
            question,
            "Administrator review snapshot must not be null");
    }
}
