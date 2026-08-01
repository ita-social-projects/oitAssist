package com.itasocialacademy.oitassist.chat.mapper;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;

import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AdminQuestionInboxMapperTest {

    private static final Long QUESTION_ID = 10L;
    private static final Long TASK_ASSIGNMENT_ID = 20L;
    private static final Long AUTHOR_ID = 30L;
    private static final Long REVIEWER_ID = 40L;
    private static final Long VERSION = 3L;

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-01T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse("2026-08-01T10:15:00Z");

    private QuestionThreadMapper questionThreadMapper;

    @BeforeEach
    void setUp() {
        questionThreadMapper =
            Mappers.getMapper(
                QuestionThreadMapper.class);
    }

    @Test
    void toAdminInboxItemResponse_question_shouldMapAllInboxFields() {
        QuestionThread question = QuestionThread.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(AUTHOR_ID)
            .assignedReviewerId(REVIEWER_ID)
            .title("Question title")
            .content("Full question content")
            .status(IN_REVIEW)
            .state(OPEN)
            .visibility(PRIVATE)
            .version(VERSION)
            .createdAt(CREATED_AT)
            .updatedAt(UPDATED_AT)
            .build();

        AdminQuestionInboxItemResponseDTO result =
            questionThreadMapper
                .toAdminInboxItemResponse(question);

        assertThat(result).isNotNull();
        assertThat(result.id())
            .isEqualTo(QUESTION_ID);
        assertThat(result.taskAssignmentId())
            .isEqualTo(TASK_ASSIGNMENT_ID);
        assertThat(result.authorId())
            .isEqualTo(AUTHOR_ID);
        assertThat(result.assignedReviewerId())
            .isEqualTo(REVIEWER_ID);
        assertThat(result.title())
            .isEqualTo("Question title");
        assertThat(result.status())
            .isEqualTo(IN_REVIEW);
        assertThat(result.state())
            .isEqualTo(OPEN);
        assertThat(result.visibility())
            .isEqualTo(PRIVATE);
        assertThat(result.version())
            .isEqualTo(VERSION);
        assertThat(result.createdAt())
            .isEqualTo(CREATED_AT);
        assertThat(result.updatedAt())
            .isEqualTo(UPDATED_AT);
    }

    @Test
    void toAdminInboxItemResponse_nullQuestion_shouldReturnNull() {
        AdminQuestionInboxItemResponseDTO result =
            questionThreadMapper
                .toAdminInboxItemResponse(null);

        assertThat(result).isNull();
    }
}