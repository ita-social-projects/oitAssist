package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.utils.QuestionAccessPolicy;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ParticipantForumServiceImplTest {

    private static final Long TASK_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;

    private static final int PAGE = 0;
    private static final int SIZE = 20;

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private QuestionAccessPolicy questionAccessPolicy;

    @InjectMocks
    private ParticipantForumServiceImpl participantForumService;

    @Test
    void getForumQuestions_accessibleTask_shouldReturnMappedPage() {
        QuestionThread publicQuestion = createPublicQuestion();
        QuestionThread privateQuestion = createPrivateQuestion();

        QuestionThreadSummaryResponseDTO publicResponse =
            createPublicQuestionResponse();
        QuestionThreadSummaryResponseDTO privateResponse =
            createPrivateQuestionResponse();

        Page<QuestionThread> repositoryPage = new PageImpl<>(
            List.of(publicQuestion, privateQuestion),
            PageRequest.of(PAGE, SIZE),
            2);

        when(questionAccessPolicy.requireTaskForumAccess(TASK_ID))
            .thenReturn(USER_ID);

        when(questionThreadRepository.findParticipantVisibleQuestions(
            eq(TASK_ID),
            eq(USER_ID),
            any(Pageable.class))).thenReturn(repositoryPage);

        when(questionThreadMapper.toSummaryResponse(publicQuestion))
            .thenReturn(publicResponse);
        when(questionThreadMapper.toSummaryResponse(privateQuestion))
            .thenReturn(privateResponse);

        Page<QuestionThreadSummaryResponseDTO> result =
            participantForumService.getForumQuestions(
                TASK_ID,
                PAGE,
                SIZE);

        assertAll(
            () -> assertEquals(
                List.of(publicResponse, privateResponse),
                result.getContent()),
            () -> assertEquals(2, result.getNumberOfElements()),
            () -> assertEquals(2, result.getTotalElements()),
            () -> assertEquals(PAGE, result.getNumber()),
            () -> assertEquals(SIZE, result.getSize()),
            () -> assertTrue(result.isFirst()),
            () -> assertTrue(result.isLast()));

        verify(questionAccessPolicy)
            .requireTaskForumAccess(TASK_ID);
        verify(questionThreadMapper)
            .toSummaryResponse(publicQuestion);
        verify(questionThreadMapper)
            .toSummaryResponse(privateQuestion);
    }

    @Test
    void getForumQuestions_shouldPassCurrentUserIdToRepository() {
        stubAccessibleEmptyForum();

        participantForumService.getForumQuestions(
            TASK_ID,
            PAGE,
            SIZE);

        verify(questionThreadRepository)
            .findParticipantVisibleQuestions(
                eq(TASK_ID),
                eq(USER_ID),
                any(Pageable.class));
    }

    @Test
    void getForumQuestions_shouldRequestPublicAndPrivateQuestions() {
        stubAccessibleEmptyForum();

        participantForumService.getForumQuestions(
            TASK_ID,
            PAGE,
            SIZE);

        verify(questionThreadRepository)
            .findParticipantVisibleQuestions(
                eq(TASK_ID),
                eq(USER_ID),
                any(Pageable.class));
    }

    @Test
    void getForumQuestions_shouldUseCreatedAtAndIdDescendingSort() {
        stubAccessibleEmptyForum();

        participantForumService.getForumQuestions(
            TASK_ID,
            PAGE,
            SIZE);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(questionThreadRepository)
            .findParticipantVisibleQuestions(
                eq(TASK_ID),
                eq(USER_ID),
                pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        List<Sort.Order> orders =
            pageable.getSort().stream().toList();

        assertAll(
            () -> assertEquals(PAGE, pageable.getPageNumber()),
            () -> assertEquals(SIZE, pageable.getPageSize()),
            () -> assertEquals(2, orders.size()),
            () -> assertEquals(
                "createdAt",
                orders.get(0).getProperty()),
            () -> assertEquals(
                Sort.Direction.DESC,
                orders.get(0).getDirection()),
            () -> assertEquals(
                "id",
                orders.get(1).getProperty()),
            () -> assertEquals(
                Sort.Direction.DESC,
                orders.get(1).getDirection()));
    }

    @Test
    void getForumQuestions_emptyResult_shouldReturnEmptyPage() {
        Page<QuestionThread> emptyPage = new PageImpl<>(
            List.of(),
            PageRequest.of(PAGE, SIZE),
            0);

        when(questionAccessPolicy.requireTaskForumAccess(TASK_ID))
            .thenReturn(USER_ID);

        when(questionThreadRepository.findParticipantVisibleQuestions(
            eq(TASK_ID),
            eq(USER_ID),
            any(Pageable.class))).thenReturn(emptyPage);

        Page<QuestionThreadSummaryResponseDTO> result =
            participantForumService.getForumQuestions(
                TASK_ID,
                PAGE,
                SIZE);

        assertAll(
            () -> assertTrue(result.isEmpty()),
            () -> assertEquals(0, result.getTotalElements()),
            () -> assertEquals(0, result.getTotalPages()),
            () -> assertEquals(PAGE, result.getNumber()),
            () -> assertEquals(SIZE, result.getSize()));

        verifyNoInteractions(questionThreadMapper);
    }

    @Test
    void getForumQuestions_unauthenticated_shouldNotQueryRepository() {
        AuthenticationException exception =
            new AuthenticationException(
                "Authentication is required to access the question forum",
                ErrorCode.AUTHENTICATION_REQUIRED);

        when(questionAccessPolicy.requireTaskForumAccess(TASK_ID))
            .thenThrow(exception);

        assertThrows(
            AuthenticationException.class,
            () -> participantForumService.getForumQuestions(
                TASK_ID,
                PAGE,
                SIZE));

        verify(questionAccessPolicy)
            .requireTaskForumAccess(TASK_ID);
        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getForumQuestions_missingTask_shouldNotQueryRepository() {
        when(questionAccessPolicy.requireTaskForumAccess(TASK_ID))
            .thenThrow(new TaskNotFoundException(TASK_ID));

        assertThrows(
            TaskNotFoundException.class,
            () -> participantForumService.getForumQuestions(
                TASK_ID,
                PAGE,
                SIZE));

        verify(questionAccessPolicy)
            .requireTaskForumAccess(TASK_ID);
        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getForumQuestions_invalidTaskId_shouldRejectBeforeAccessCheck() {
        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantForumService.getForumQuestions(
                    null,
                    PAGE,
                    SIZE)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantForumService.getForumQuestions(
                    0L,
                    PAGE,
                    SIZE)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantForumService.getForumQuestions(
                    -1L,
                    PAGE,
                    SIZE)));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getForumQuestions_negativePage_shouldRejectBeforeAccessCheck() {
        assertThrows(
            ValidationException.class,
            () -> participantForumService.getForumQuestions(
                TASK_ID,
                -1,
                SIZE));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getForumQuestions_invalidSize_shouldRejectBeforeAccessCheck() {
        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantForumService.getForumQuestions(
                    TASK_ID,
                    PAGE,
                    0)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantForumService.getForumQuestions(
                    TASK_ID,
                    PAGE,
                    101)));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository,
            questionThreadMapper);
    }

    private void stubAccessibleEmptyForum() {
        when(questionAccessPolicy.requireTaskForumAccess(TASK_ID))
            .thenReturn(USER_ID);

        when(questionThreadRepository.findParticipantVisibleQuestions(
            eq(TASK_ID),
            eq(USER_ID),
            any(Pageable.class))).thenReturn(Page.empty());
    }

    private QuestionThread createPublicQuestion() {
        Instant createdAt =
            Instant.parse("2026-07-24T10:00:00Z");
        Instant updatedAt =
            Instant.parse("2026-07-24T10:15:00Z");

        return QuestionThread.builder()
            .id(11L)
            .taskId(TASK_ID)
            .authorId(OTHER_USER_ID)
            .title("Public question")
            .content("Public question content")
            .status(NEW)
            .state(OPEN)
            .visibility(PUBLIC)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    private QuestionThread createPrivateQuestion() {
        Instant createdAt =
            Instant.parse("2026-07-24T11:00:00Z");
        Instant updatedAt =
            Instant.parse("2026-07-24T11:30:00Z");

        return QuestionThread.builder()
            .id(12L)
            .taskId(TASK_ID)
            .authorId(USER_ID)
            .title("Private question")
            .content("Private question content")
            .status(ANSWERED)
            .state(CLOSED)
            .visibility(PRIVATE)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    private QuestionThreadSummaryResponseDTO createPublicQuestionResponse() {

        return new QuestionThreadSummaryResponseDTO(
            11L,
            TASK_ID,
            OTHER_USER_ID,
            "Public question",
            NEW,
            PUBLIC,
            OPEN,
            Instant.parse("2026-07-24T10:00:00Z"),
            Instant.parse("2026-07-24T10:15:00Z"));
    }

    private QuestionThreadSummaryResponseDTO createPrivateQuestionResponse() {

        return new QuestionThreadSummaryResponseDTO(
            12L,
            TASK_ID,
            USER_ID,
            "Private question",
            ANSWERED,
            PRIVATE,
            CLOSED,
            Instant.parse("2026-07-24T11:00:00Z"),
            Instant.parse("2026-07-24T11:30:00Z"));
    }
}