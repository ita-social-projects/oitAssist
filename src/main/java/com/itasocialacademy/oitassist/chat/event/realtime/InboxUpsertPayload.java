package com.itasocialacademy.oitassist.chat.event.realtime;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import java.util.Objects;

public record InboxUpsertPayload(QuestionReviewInboxItemResponseDTO question) implements RealtimePayload {
    public InboxUpsertPayload {
        Objects.requireNonNull(question, "Administrator inbox snapshot must not be null");
    }
}