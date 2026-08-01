package com.itasocialacademy.oitassist.chat.mapper;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static org.assertj.core.api.Assertions.assertThat;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class QuestionMessageMapperTest {

    private static final Long MESSAGE_ID = 1L;
    private static final Long QUESTION_ID = 2L;
    private static final Long AUTHOR_ID = 3L;

    private static final String CONTENT =
        "Question message content";

    private static final Instant CREATED_AT =
        Instant.parse("2026-07-27T10:00:00Z");

    private QuestionMessageMapper questionMessageMapper;

    @BeforeEach
    void setUp() {
        questionMessageMapper =
            Mappers.getMapper(
                QuestionMessageMapper.class);
    }

    @Test
    void toResponse_comment_shouldMapAllFields() {
        QuestionMessage message =
            createMessage(COMMENT);

        QuestionMessageResponseDTO result =
            questionMessageMapper.toResponse(message);

        assertThat(result).isNotNull();
        assertThat(result.id())
            .isEqualTo(MESSAGE_ID);
        assertThat(result.questionThreadId())
            .isEqualTo(QUESTION_ID);
        assertThat(result.authorId())
            .isEqualTo(AUTHOR_ID);
        assertThat(result.type())
            .isEqualTo(COMMENT);
        assertThat(result.content())
            .isEqualTo(CONTENT);
        assertThat(result.createdAt())
            .isEqualTo(CREATED_AT);
    }

    @Test
    void toResponse_officialAnswer_shouldPreserveMessageType() {
        QuestionMessage message =
            createMessage(OFFICIAL_ANSWER);

        QuestionMessageResponseDTO result =
            questionMessageMapper.toResponse(message);

        assertThat(result).isNotNull();
        assertThat(result.type())
            .isEqualTo(OFFICIAL_ANSWER);
    }

    @Test
    void toResponse_nullMessage_shouldReturnNull() {
        QuestionMessageResponseDTO result =
            questionMessageMapper.toResponse(null);

        assertThat(result).isNull();
    }

    private QuestionMessage createMessage(
        QuestionMessageType type) {

        return QuestionMessage.builder()
            .id(MESSAGE_ID)
            .questionThreadId(QUESTION_ID)
            .authorId(AUTHOR_ID)
            .type(type)
            .content(CONTENT)
            .createdAt(CREATED_AT)
            .build();
    }
}