package com.itasocialacademy.oitassist.chat.mapper;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.assertj.core.api.Assertions.assertThat;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class QuestionThreadMapperTest {

    private static final Long QUESTION_ID = 11L;
    private static final Long TASK_ID = 1L;
    private static final Long AUTHOR_ID = 100L;
    private static final Long REVIEWER_ID = 200L;
    private static final Long VERSION = 3L;

    private static final String TITLE = "How should I submit the solution?";
    private static final String CONTENT = "Should the solution be submitted as a PDF file?";

    private static final Instant CREATED_AT = Instant.parse("2026-07-24T10:00:00Z");

    private static final Instant UPDATED_AT = Instant.parse("2026-07-24T10:15:00Z");

    private QuestionThreadMapper questionThreadMapper;

    @BeforeEach
    void setUp() {
        questionThreadMapper =
            Mappers.getMapper(QuestionThreadMapper.class);
    }

    @Test
    void toResponse_questionThread_shouldMapAllFields() {
        QuestionThread questionThread = createQuestionThread();

        QuestionThreadResponseDTO result =
            questionThreadMapper.toResponse(questionThread);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(QUESTION_ID);
        assertThat(result.taskId()).isEqualTo(TASK_ID);
        assertThat(result.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(result.assignedReviewerId())
            .isEqualTo(REVIEWER_ID);
        assertThat(result.title()).isEqualTo(TITLE);
        assertThat(result.content()).isEqualTo(CONTENT);
        assertThat(result.status()).isEqualTo(ANSWERED);
        assertThat(result.state()).isEqualTo(CLOSED);
        assertThat(result.visibility()).isEqualTo(PUBLIC);
        assertThat(result.version()).isEqualTo(VERSION);
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void toSummaryResponse_questionThread_shouldMapSummaryFields() {
        QuestionThread questionThread = createQuestionThread();

        QuestionThreadSummaryResponseDTO result =
            questionThreadMapper.toSummaryResponse(questionThread);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(QUESTION_ID);
        assertThat(result.taskId()).isEqualTo(TASK_ID);
        assertThat(result.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(result.title()).isEqualTo(TITLE);
        assertThat(result.status()).isEqualTo(ANSWERED);
        assertThat(result.state()).isEqualTo(CLOSED);
        assertThat(result.visibility()).isEqualTo(PUBLIC);
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void toResponse_nullQuestionThread_shouldReturnNull() {
        QuestionThreadResponseDTO result =
            questionThreadMapper.toResponse(null);

        assertThat(result).isNull();
    }

    @Test
    void toSummaryResponse_nullQuestionThread_shouldReturnNull() {
        QuestionThreadSummaryResponseDTO result =
            questionThreadMapper.toSummaryResponse(null);

        assertThat(result).isNull();
    }

    private QuestionThread createQuestionThread() {
        return QuestionThread.builder()
            .id(QUESTION_ID)
            .taskId(TASK_ID)
            .authorId(AUTHOR_ID)
            .assignedReviewerId(REVIEWER_ID)
            .title(TITLE)
            .content(CONTENT)
            .status(ANSWERED)
            .state(CLOSED)
            .visibility(PUBLIC)
            .version(VERSION)
            .createdAt(CREATED_AT)
            .updatedAt(UPDATED_AT)
            .build();
    }

    @Test
    void toEntity_validRequest_shouldMapTitleAndContent() {
        CreateQuestionRequestDTO request =
                new CreateQuestionRequestDTO(TITLE, CONTENT);

        QuestionThread result =
                questionThreadMapper.toEntity(request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(TITLE);
        assertThat(result.getContent()).isEqualTo(CONTENT);
    }

    @Test
    void toEntity_validRequest_shouldNotMapProtectedFields() {
        CreateQuestionRequestDTO request =
                new CreateQuestionRequestDTO(TITLE, CONTENT);

        QuestionThread result =
                questionThreadMapper.toEntity(request);

        assertThat(result).isNotNull();

        assertThat(result.getId()).isNull();
        assertThat(result.getTaskId()).isNull();
        assertThat(result.getAuthorId()).isNull();
        assertThat(result.getAssignedReviewerId()).isNull();

        assertThat(result.getStatus()).isEqualTo(NEW);
        assertThat(result.getState()).isEqualTo(OPEN);
        assertThat(result.getVisibility()).isEqualTo(PRIVATE);
        assertThat(result.getVersion()).isZero();

        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_nullRequest_shouldReturnNull() {
        QuestionThread result =
                questionThreadMapper.toEntity(null);

        assertThat(result).isNull();
    }
}