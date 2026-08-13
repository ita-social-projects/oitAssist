package com.itasocialacademy.oitassist.chat.realtime.event;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import java.util.Objects;

/**
 * Contains an administrator inbox item that must be inserted or replaced.
 */
public record InboxUpsertPayload(
    QuestionReviewInboxItemResponseDTO question)
    implements RealtimePayload {
    public InboxUpsertPayload {
        Objects.requireNonNull(
            question,
            "Administrator inbox snapshot must not be null");
    }
}
