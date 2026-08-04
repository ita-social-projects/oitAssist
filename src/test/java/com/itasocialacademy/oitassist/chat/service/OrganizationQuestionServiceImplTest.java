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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class OrganizationQuestionServiceImplTest {

    private static final String ORG_ROLE = "ORG";

    private static final Long RESPONDER_ID = 10L;
    private static final Long OTHER_RESPONDER_ID = 11L;

    private static final Long PRIVATE_QUESTION_ID = 20L;
    private static final Long PUBLIC_QUESTION_ID = 21L;

    private static final int PAGE = 0;
    private static final int SIZE = 20;

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-05T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse("2026-08-05T10:15:00Z");

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private SecurityFacade securityFacade;

    @InjectMocks
    private OrganizationQuestionServiceImpl organizationQuestionService;

    @Test
    void getResponderInbox_eligibleOrg_shouldReturnMappedPage() {

        stubOrganizationMember();

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

        when(questionThreadRepository
            .findResponderUnclaimedQuestions(
                eq(RESPONDER_ID),
                eq(OPEN),
                eq(NEW),
                any(Pageable.class)))
            .thenReturn(
                new PageImpl<>(
                    List.of(
                        privateQuestion,
                        publicQuestion),
                    PageRequest.of(
                        PAGE,
                        SIZE),
                    2));

        when(questionThreadMapper
            .toReviewInboxItemResponse(
                privateQuestion))
            .thenReturn(privateResponse);

        when(questionThreadMapper
            .toReviewInboxItemResponse(
                publicQuestion))
            .thenReturn(publicResponse);

        Page<QuestionReviewInboxItemResponseDTO> result =
            organizationQuestionService
                .getResponderInbox(
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

        verify(questionThreadRepository)
            .findResponderUnclaimedQuestions(
                eq(RESPONDER_ID),
                eq(OPEN),
                eq(NEW),
                any(Pageable.class));

        verify(questionThreadRepository, never())
            .findAllByStateAndStatusAndAssignedReviewerIdIsNull(
                any(),
                any(),
                any());
    }

    @Test
    void getResponderInbox_shouldUseCurrentUserPaginationAndOrdering() {

        stubOrganizationMember();

        when(questionThreadRepository
            .findResponderUnclaimedQuestions(
                eq(RESPONDER_ID),
                eq(OPEN),
                eq(NEW),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        2,
                        15)));

        organizationQuestionService
            .getResponderInbox(
                2,
                15);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(
                Pageable.class);

        verify(questionThreadRepository)
            .findResponderUnclaimedQuestions(
                eq(RESPONDER_ID),
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
    void getResponderInbox_orgWithoutAssignments_shouldReturnEmptyPage() {

        stubOrganizationMember();

        when(questionThreadRepository
            .findResponderUnclaimedQuestions(
                eq(RESPONDER_ID),
                eq(OPEN),
                eq(NEW),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        PAGE,
                        SIZE)));

        Page<QuestionReviewInboxItemResponseDTO> result =
            organizationQuestionService
                .getResponderInbox(
                    PAGE,
                    SIZE);

        assertAll(
            () -> assertTrue(
                result.isEmpty()),
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
    void getAssignedToCurrentResponder_withoutStatus_shouldUseCurrentUser() {

        stubOrganizationMember();

        QuestionThread question =
            createQuestion(
                PRIVATE_QUESTION_ID,
                RESPONDER_ID,
                ANSWERED,
                PRIVATE);

        QuestionReviewInboxItemResponseDTO response =
            createResponse(question);

        when(questionThreadRepository
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(RESPONDER_ID),
                any(Pageable.class)))
            .thenReturn(
                new PageImpl<>(
                    List.of(question),
                    PageRequest.of(
                        PAGE,
                        SIZE),
                    1));

        when(questionThreadMapper
            .toReviewInboxItemResponse(
                question))
            .thenReturn(response);

        Page<QuestionReviewInboxItemResponseDTO> result =
            organizationQuestionService
                .getAssignedToCurrentResponder(
                    null,
                    PAGE,
                    SIZE);

        assertAll(
            () -> assertEquals(
                1,
                result.getTotalElements()),
            () -> assertSame(
                response,
                result.getContent()
                    .getFirst()));

        verify(questionThreadRepository)
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(RESPONDER_ID),
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
    void getAssignedToCurrentResponder_statusFilter_shouldDelegateExactStatus(
        QuestionStatus status) {

        stubOrganizationMember();

        when(questionThreadRepository
            .findAllByStateAndAssignedReviewerIdAndStatus(
                eq(OPEN),
                eq(RESPONDER_ID),
                eq(status),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        PAGE,
                        SIZE)));

        organizationQuestionService
            .getAssignedToCurrentResponder(
                status,
                PAGE,
                SIZE);

        verify(questionThreadRepository)
            .findAllByStateAndAssignedReviewerIdAndStatus(
                eq(OPEN),
                eq(RESPONDER_ID),
                eq(status),
                any(Pageable.class));

        verify(questionThreadRepository, never())
            .findAllByStateAndAssignedReviewerId(
                any(),
                any(),
                any());
    }

    @Test
    void getAssignedToCurrentResponder_shouldUseDescendingOrdering() {

        stubOrganizationMember();

        when(questionThreadRepository
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(RESPONDER_ID),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        3,
                        12)));

        organizationQuestionService
            .getAssignedToCurrentResponder(
                null,
                3,
                12);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(
                Pageable.class);

        verify(questionThreadRepository)
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(RESPONDER_ID),
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
    void getAssignedToCurrentResponder_shouldNeverQueryAnotherReviewer() {

        stubOrganizationMember();

        when(questionThreadRepository
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(RESPONDER_ID),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        PAGE,
                        SIZE)));

        organizationQuestionService
            .getAssignedToCurrentResponder(
                null,
                PAGE,
                SIZE);

        verify(questionThreadRepository)
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(RESPONDER_ID),
                any(Pageable.class));

        verify(questionThreadRepository, never())
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(OTHER_RESPONDER_ID),
                any(Pageable.class));
    }

    @Test
    void getAssignedToCurrentResponder_emptyResult_shouldReturnEmptyPage() {

        stubOrganizationMember();

        when(questionThreadRepository
            .findAllByStateAndAssignedReviewerId(
                eq(OPEN),
                eq(RESPONDER_ID),
                any(Pageable.class)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        PAGE,
                        SIZE)));

        Page<QuestionReviewInboxItemResponseDTO> result =
            organizationQuestionService
                .getAssignedToCurrentResponder(
                    null,
                    PAGE,
                    SIZE);

        assertTrue(result.isEmpty());

        verifyNoInteractions(
            questionThreadMapper);
    }

    @Test
    void getResponderInbox_unauthenticated_shouldRejectBeforeRepository() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> organizationQuestionService
                .getResponderInbox(
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
    void getAssignedToCurrentResponder_unauthenticated_shouldReject() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> organizationQuestionService
                .getAssignedToCurrentResponder(
                    null,
                    PAGE,
                    SIZE));

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getResponderInbox_nonOrg_shouldRejectBeforeRepository() {

        stubNonOrganizationMember();

        assertThrows(
            AuthorizationException.class,
            () -> organizationQuestionService
                .getResponderInbox(
                    PAGE,
                    SIZE));

        verify(securityFacade)
            .hasRole(ORG_ROLE);

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getAssignedToCurrentResponder_globalAdminWithoutOrg_shouldReject() {

        stubNonOrganizationMember();

        assertThrows(
            AuthorizationException.class,
            () -> organizationQuestionService
                .getAssignedToCurrentResponder(
                    IN_REVIEW,
                    PAGE,
                    SIZE));

        verify(securityFacade)
            .hasRole(ORG_ROLE);

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void queueOperations_invalidPagination_shouldRejectBeforeSecurity() {

        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> organizationQuestionService
                    .getResponderInbox(
                        -1,
                        SIZE)),
            () -> assertThrows(
                ValidationException.class,
                () -> organizationQuestionService
                    .getResponderInbox(
                        PAGE,
                        0)),
            () -> assertThrows(
                ValidationException.class,
                () -> organizationQuestionService
                    .getAssignedToCurrentResponder(
                        null,
                        PAGE,
                        MAX_PAGE_SIZE + 1)));

        verifyNoInteractions(
            securityFacade,
            questionThreadRepository,
            questionThreadMapper);
    }

    private void stubOrganizationMember() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(
                Optional.of(
                    RESPONDER_ID));

        when(securityFacade.hasRole(ORG_ROLE))
            .thenReturn(true);
    }

    private void stubNonOrganizationMember() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(
                Optional.of(
                    RESPONDER_ID));

        when(securityFacade.hasRole(ORG_ROLE))
            .thenReturn(false);
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
            .assignedReviewerId(
                assignedReviewerId)
            .title(
                "Question " + questionId)
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