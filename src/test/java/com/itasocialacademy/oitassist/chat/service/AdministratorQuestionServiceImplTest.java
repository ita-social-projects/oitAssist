package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AdministratorQuestionServiceImplTest {

    private static final String ADMIN_ROLE = "ADMIN";

    private static final Long ADMINISTRATOR_ID = 10L;
    private static final Long OTHER_ADMINISTRATOR_ID = 11L;

    private static final Long PRIVATE_QUESTION_ID = 20L;
    private static final Long PUBLIC_QUESTION_ID = 21L;

    private static final int PAGE = 0;
    private static final int SIZE = 20;

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-01T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse("2026-08-01T10:15:00Z");

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AdministratorQuestionServiceImpl administratorQuestionService;

    @Test
    void getUnclaimedQuestions_administrator_shouldReturnMappedPage() {
        stubAdministrator();

        QuestionThread privateQuestion =
            createQuestion(
                PRIVATE_QUESTION_ID,
                null,
                NEW,
                PRIVATE);

        QuestionThread publicQuestion =
            createQuestion(
                PUBLIC_QUESTION_ID,
                null,
                NEW,
                PUBLIC);

        QuestionReviewInboxItemResponseDTO privateResponse =
            createResponse(privateQuestion);

        QuestionReviewInboxItemResponseDTO publicResponse =
            createResponse(publicQuestion);

        Page<QuestionThread> repositoryPage =
            new PageImpl<>(
                List.of(
                    privateQuestion,
                    publicQuestion),
                PageRequest.of(PAGE, SIZE),
                2);

        when(questionThreadRepository
            .findAllByStateAndStatusAndAssignedReviewerIdIsNull(
                eq(OPEN),
                eq(NEW),
                any(Pageable.class)))
            .thenReturn(repositoryPage);

        when(questionThreadMapper
            .toReviewInboxItemResponse(privateQuestion))
            .thenReturn(privateResponse);

        when(questionThreadMapper
            .toReviewInboxItemResponse(publicQuestion))
            .thenReturn(publicResponse);

        Page<QuestionReviewInboxItemResponseDTO> result =
            administratorQuestionService
                .getUnclaimedQuestions(
                    PAGE,
                    SIZE);

        assertAll(
            () -> assertEquals(
                List.of(
                    privateResponse,
                    publicResponse),
                result.getContent()),
            () -> assertEquals(
                2,
                result.getTotalElements()),
            () -> assertEquals(
                PRIVATE,
                result.getContent()
                    .get(0)
                    .visibility()),
            () -> assertEquals(
                PUBLIC,
                result.getContent()
                    .get(1)
                    .visibility()));

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));
    }

    @Test
    void getUnclaimedQuestions_shouldUseRequiredFilterPaginationAndOrdering() {
        stubAdministrator();

        when(questionThreadRepository
            .findAllByStateAndStatusAndAssignedReviewerIdIsNull(
                eq(OPEN),
                eq(NEW),
                any(Pageable.class)))
            .thenReturn(Page.empty(
                PageRequest.of(2, 15)));

        administratorQuestionService
            .getUnclaimedQuestions(
                2,
                15);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(
                Pageable.class);

        verify(questionThreadRepository)
            .findAllByStateAndStatusAndAssignedReviewerIdIsNull(
                eq(OPEN),
                eq(NEW),
                pageableCaptor.capture());

        Pageable pageable =
            pageableCaptor.getValue();

        List<Sort.Order> orders =
            pageable.getSort()
                .stream()
                .toList();

        assertAll(
            () -> assertEquals(
                2,
                pageable.getPageNumber()),
            () -> assertEquals(
                15,
                pageable.getPageSize()),
            () -> assertEquals(
                2,
                orders.size()),
            () -> assertEquals(
                "createdAt",
                orders.get(0).getProperty()),
            () -> assertEquals(
                Sort.Direction.ASC,
                orders.get(0).getDirection()),
            () -> assertEquals(
                "id",
                orders.get(1).getProperty()),
            () -> assertEquals(
                Sort.Direction.ASC,
                orders.get(1).getDirection()));
    }

    @Test
    void getUnclaimedQuestions_emptyResult_shouldReturnEmptyPage() {
        stubAdministrator();

        when(questionThreadRepository
            .findAllByStateAndStatusAndAssignedReviewerIdIsNull(
                eq(OPEN),
                eq(NEW),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        PAGE,
                        SIZE)));

        Page<QuestionReviewInboxItemResponseDTO> result =
            administratorQuestionService
                .getUnclaimedQuestions(
                    PAGE,
                    SIZE);

        assertAll(
            () -> assertEquals(
                0,
                result.getTotalElements()),
            () -> assertEquals(
                PAGE,
                result.getNumber()),
            () -> assertEquals(
                SIZE,
                result.getSize()));

        verifyNoInteractions(
            questionThreadMapper);
    }

    @Test
    void getAssignedQuestions_withoutStatus_shouldUseCurrentAdministrator() {
        stubAdministrator();

        QuestionThread question =
            createQuestion(
                PRIVATE_QUESTION_ID,
                ADMINISTRATOR_ID,
                ANSWERED,
                PRIVATE);

        QuestionReviewInboxItemResponseDTO response =
            createResponse(question);

        when(questionThreadRepository
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(ADMINISTRATOR_ID),
                any(Pageable.class)))
            .thenReturn(new PageImpl<>(
                List.of(question),
                PageRequest.of(PAGE, SIZE),
                1));

        when(questionThreadMapper
            .toReviewInboxItemResponse(question))
            .thenReturn(response);

        Page<QuestionReviewInboxItemResponseDTO> result =
            administratorQuestionService
                .getAssignedQuestions(
                    null,
                    PAGE,
                    SIZE);

        assertAll(
            () -> assertEquals(
                1,
                result.getTotalElements()),
            () -> assertSame(
                response,
                result.getContent().getFirst()));

        verify(questionThreadRepository)
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(ADMINISTRATOR_ID),
                any(Pageable.class));

        verify(questionThreadRepository, never())
            .findAllByStateAndAssignedReviewerIdAndStatus(
                any(),
                any(),
                any(),
                any());
    }

    @ParameterizedTest
    @EnumSource(QuestionStatus.class)
    void getAssignedQuestions_statusFilter_shouldDelegateExactStatus(
        QuestionStatus status) {

        stubAdministrator();

        when(questionThreadRepository
            .findAllByStateAndAssignedReviewerIdAndStatus(
                eq(OPEN),
                eq(ADMINISTRATOR_ID),
                eq(status),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        PAGE,
                        SIZE)));

        administratorQuestionService
            .getAssignedQuestions(
                status,
                PAGE,
                SIZE);

        verify(questionThreadRepository)
            .findAllByStateAndAssignedReviewerIdAndStatus(
                eq(OPEN),
                eq(ADMINISTRATOR_ID),
                eq(status),
                any(Pageable.class));

        verify(questionThreadRepository, never())
            .findAllByStateAndAssignedReviewerId(
                any(),
                any(),
                any());
    }

    @Test
    void getAssignedQuestions_shouldUseUpdatedAtAndIdDescendingOrdering() {
        stubAdministrator();

        when(questionThreadRepository
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(ADMINISTRATOR_ID),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        3,
                        12)));

        administratorQuestionService
            .getAssignedQuestions(
                null,
                3,
                12);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(
                Pageable.class);

        verify(questionThreadRepository)
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(ADMINISTRATOR_ID),
                pageableCaptor.capture());

        Pageable pageable =
            pageableCaptor.getValue();

        List<Sort.Order> orders =
            pageable.getSort()
                .stream()
                .toList();

        assertAll(
            () -> assertEquals(
                3,
                pageable.getPageNumber()),
            () -> assertEquals(
                12,
                pageable.getPageSize()),
            () -> assertEquals(
                "updatedAt",
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
    void getAssignedQuestions_questionAssignedToAnotherAdmin_shouldQueryOnlyCurrentAdminId() {
        stubAdministrator();

        when(questionThreadRepository
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(ADMINISTRATOR_ID),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        PAGE,
                        SIZE)));

        administratorQuestionService
            .getAssignedQuestions(
                null,
                PAGE,
                SIZE);

        verify(questionThreadRepository)
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(ADMINISTRATOR_ID),
                any(Pageable.class));

        verify(questionThreadRepository, never())
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(OTHER_ADMINISTRATOR_ID),
                any(Pageable.class));
    }

    @Test
    void getAssignedQuestions_emptyResult_shouldReturnEmptyPage() {
        stubAdministrator();

        when(questionThreadRepository
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(ADMINISTRATOR_ID),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        PAGE,
                        SIZE)));

        Page<QuestionReviewInboxItemResponseDTO> result =
            administratorQuestionService
                .getAssignedQuestions(
                    null,
                    PAGE,
                    SIZE);

        assertEquals(
            0,
            result.getTotalElements());

        verifyNoInteractions(
            questionThreadMapper);
    }

    @Test
    void getUnclaimedQuestions_unauthenticated_shouldRejectBeforeRepository() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> administratorQuestionService
                .getUnclaimedQuestions(
                    PAGE,
                    SIZE));

        verify(
            securityFacade,
            never())
            .hasRole(anyString());

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getAssignedQuestions_nonAdministrator_shouldRejectBeforeRepository() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(
                Optional.of(
                    ADMINISTRATOR_ID));

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> administratorQuestionService
                .getAssignedQuestions(
                    null,
                    PAGE,
                    SIZE));

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getAssignedQuestions_orgWithoutAdmin_shouldReject() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(
                Optional.of(
                    ADMINISTRATOR_ID));

        when(securityFacade.hasRole(anyString()))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> administratorQuestionService
                .getAssignedQuestions(
                    IN_REVIEW,
                    PAGE,
                    SIZE));

        verify(securityFacade)
            .hasRole(ADMIN_ROLE);

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void inboxOperations_invalidPagination_shouldRejectBeforeSecurityAndRepository() {
        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> administratorQuestionService
                    .getUnclaimedQuestions(
                        -1,
                        SIZE)),
            () -> assertThrows(
                ValidationException.class,
                () -> administratorQuestionService
                    .getUnclaimedQuestions(
                        PAGE,
                        0)),
            () -> assertThrows(
                ValidationException.class,
                () -> administratorQuestionService
                    .getAssignedQuestions(
                        null,
                        PAGE,
                        MAX_PAGE_SIZE + 1)));

        verifyNoInteractions(
            securityFacade,
            questionThreadRepository,
            questionThreadMapper);
    }

    private void stubAdministrator() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(
                Optional.of(
                    ADMINISTRATOR_ID));

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(true);
    }

    private QuestionThread createQuestion(
        Long questionId,
        Long assignedReviewerId,
        QuestionStatus status,
        QuestionVisibility visibility) {

        return QuestionThread.builder()
            .id(questionId)
            .taskAssignmentId(100L)
            .authorId(200L)
            .assignedReviewerId(assignedReviewerId)
            .title("Question " + questionId)
            .content("Question content")
            .status(status)
            .state(OPEN)
            .visibility(visibility)
            .version(2L)
            .createdAt(CREATED_AT)
            .updatedAt(UPDATED_AT)
            .build();
    }

    private QuestionReviewInboxItemResponseDTO createResponse(
        QuestionThread question) {

        return new QuestionReviewInboxItemResponseDTO(
            question.getId(),
            question.getTaskAssignmentId(),
            question.getAuthorId(),
            question.getAssignedReviewerId(),
            question.getTitle(),
            question.getStatus(),
            question.getState(),
            question.getVisibility(),
            question.getVersion(),
            question.getCreatedAt(),
            question.getUpdatedAt());
    }
}