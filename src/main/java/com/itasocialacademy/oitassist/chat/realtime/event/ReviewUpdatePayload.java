package com.itasocialacademy.oitassist.chat.realtime.event;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import java.util.Objects;

/**
 * Contains the current question snapshot for the assigned administrator's
 * review projection.
 */
public record ReviewUpdatePayload(
    QuestionReviewInboxItemResponseDTO question)
    implements RealtimePayload {
    public ReviewUpdatePayload {
        Objects.requireNonNull(
            question,
            "Administrator review snapshot must not be null");
    }
}
