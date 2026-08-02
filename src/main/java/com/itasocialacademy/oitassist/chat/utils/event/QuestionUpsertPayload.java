package com.itasocialacademy.oitassist.chat.utils.event;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import java.util.Objects;

/**
 * Contains the complete question snapshot required to insert or replace a
 * question projection.
 */
public record QuestionUpsertPayload(
    QuestionThreadResponseDTO question)
    implements RealtimePayload {
    public QuestionUpsertPayload {
        Objects.requireNonNull(
            question,
            "Question snapshot must not be null");
    }
}