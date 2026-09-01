package com.itasocialacademy.oitassist.chat.event.realtime;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import java.util.Objects;

public record QuestionUpsertPayload(QuestionThreadResponseDTO question) implements RealtimePayload {
    public QuestionUpsertPayload {
        Objects.requireNonNull(question, "Question snapshot must not be null");
    }
}