package com.itasocialacademy.oitassist.chat.mapper;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class QuestionReviewInboxMapperTest {

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-05T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse("2026-08-05T10:15:00Z");

    private final QuestionThreadMapper mapper =
        Mappers.getMapper(
            QuestionThreadMapper.class);

    @Test
    void toReviewInboxItemResponse_shouldMapAllQueueFields() {

        QuestionThread question =
            QuestionThread.builder()
                .id(1L)
                .taskAssignmentId(10L)
                .authorId(20L)
                .assignedReviewerId(30L)
                .title("Clarification")
                .content("Protected full content")
                .status(IN_REVIEW)
                .state(OPEN)
                .visibility(PRIVATE)
                .version(4L)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();

        QuestionReviewInboxItemResponseDTO result =
            mapper.toReviewInboxItemResponse(
                question);

        assertAll(
            () -> assertEquals(
                1L,
                result.id()),
            () -> assertEquals(
                10L,
                result.taskAssignmentId()),
            () -> assertEquals(
                20L,
                result.authorId()),
            () -> assertEquals(
                30L,
                result.assignedReviewerId()),
            () -> assertEquals(
                "Clarification",
                result.title()),
            () -> assertEquals(
                IN_REVIEW,
                result.status()),
            () -> assertEquals(
                OPEN,
                result.state()),
            () -> assertEquals(
                PRIVATE,
                result.visibility()),
            () -> assertEquals(
                4L,
                result.version()),
            () -> assertEquals(
                CREATED_AT,
                result.createdAt()),
            () -> assertEquals(
                UPDATED_AT,
                result.updatedAt()));
    }
}