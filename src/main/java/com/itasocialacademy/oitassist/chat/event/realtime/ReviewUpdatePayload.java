package com.itasocialacademy.oitassist.chat.event.realtime;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import java.util.Objects;

public record ReviewUpdatePayload(QuestionReviewInboxItemResponseDTO question) implements RealtimePayload {
    public ReviewUpdatePayload {
        Objects.requireNonNull(question, "Administrator review snapshot must not be null");
    }
}