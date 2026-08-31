package com.itasocialacademy.oitassist.chat.realtime.event;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import java.util.Objects;

/**
 * Contains the created message required for direct append to a thread cache.
 */
public record MessageCreatedPayload(
    QuestionMessageResponseDTO message)
    implements RealtimePayload {
    public MessageCreatedPayload {
        Objects.requireNonNull(
            message,
            "Created message must not be null");
    }
}