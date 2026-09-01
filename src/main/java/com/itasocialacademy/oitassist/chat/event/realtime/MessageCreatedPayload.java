package com.itasocialacademy.oitassist.chat.event.realtime;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import java.util.Objects;

public record MessageCreatedPayload(QuestionMessageResponseDTO message) implements RealtimePayload {
    public MessageCreatedPayload {
        Objects.requireNonNull(message, "Created message must not be null");
    }
}