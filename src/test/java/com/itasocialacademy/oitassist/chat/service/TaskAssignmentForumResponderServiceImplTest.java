package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.Direction.DESC;

import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderGrantResult;
import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.chat.exceptions.ForumResponderAccessRestrictedException;
import com.itasocialacademy.oitassist.chat.exceptions.ForumResponderActiveReviewException;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidForumResponderCandidateException;
import com.itasocialacademy.oitassist.chat.mapper.TaskAssignmentForumResponderMapper;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import com.itasocialacademy.oitassist.user.api.dto.ForumResponderCandidate;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
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
class TaskAssignmentForumResponderServiceImplTest {

    private static final Long TASK_ASSIGNMENT_ID = 10L;
    private static final Long OTHER_TASK_ASSIGNMENT_ID = 11L;
    private static final Long TASK_BODY_ID = 100L;
    private static final Long TOUR_ID = 200L;

    private static final Long RESPONDER_ID = 20L;
    private static final Long SECOND_RESPONDER_ID = 21L;
    private static final Long ADMINISTRATOR_ID = 30L;
    private static final Long ORIGINAL_ADMINISTRATOR_ID = 31L;

    private static final Instant ASSIGNED_AT =
        Instant.parse("2026-08-04T12:00:00Z");

    private static final Instant SECOND_ASSIGNED_AT =
        Instant.parse("2026-08-04T13:00:00Z");

    @Mock
    private TaskAssignmentForumResponderRepository responderRepository;

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private TaskAssignmentForumResponderMapper responderMapper;

    @Mock
    private TaskAssignmentFacade taskAssignmentFacade;

    @Mock
    private UserFacade userFacade;

    @Mock
    private SecurityFacade securityFacade;

    @InjectMocks
    private TaskAssignmentForumResponderServiceImpl service;

    /*
     * Grant
     */

    @Test
    void grantResponder_activeOrg_shouldCreateAssignment() {

        ForumResponderCandidate candidate =
            activeOrgCandidate(RESPONDER_ID);

        TaskAssignmentForumResponder assignment =
            responderAssignment(
                1L,
                RESPONDER_ID,
                ADMINISTRATOR_ID,
                ASSIGNED_AT);

        TaskAssignmentForumResponderResponseDTO response =
            responderResponse(
                1L,
                RESPONDER_ID,
                ADMINISTRATOR_ID,
                ASSIGNED_AT,
                candidate);

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(candidate);

        when(responderRepository.insertIfAbsent(
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(ADMINISTRATOR_ID),
            any(Instant.class)))
            .thenReturn(1);

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.of(assignment));

        when(responderMapper.toResponse(
            assignment,
            candidate))
            .thenReturn(response);

        TaskAssignmentForumResponderGrantResult result =
            service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        assertTrue(result.created());
        assertEquals(response, result.responder());

        verify(responderRepository).insertIfAbsent(
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(ADMINISTRATOR_ID),
            any(Instant.class));

        verify(responderRepository)
            .findByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        verify(responderMapper).toResponse(
            assignment,
            candidate);

        verifyNoInteractions(questionThreadRepository);
    }

    @Test
    void grantResponder_existingAssignment_shouldReturnPersistedAssignment() {

        ForumResponderCandidate candidate =
            activeOrgCandidate(RESPONDER_ID);

        TaskAssignmentForumResponder existingAssignment =
            responderAssignment(
                1L,
                RESPONDER_ID,
                ORIGINAL_ADMINISTRATOR_ID,
                ASSIGNED_AT);

        TaskAssignmentForumResponderResponseDTO existingResponse =
            responderResponse(
                1L,
                RESPONDER_ID,
                ORIGINAL_ADMINISTRATOR_ID,
                ASSIGNED_AT,
                candidate);

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(candidate);

        when(responderRepository.insertIfAbsent(
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(ADMINISTRATOR_ID),
            any(Instant.class)))
            .thenReturn(0);

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.of(existingAssignment));

        when(responderMapper.toResponse(
            existingAssignment,
            candidate))
            .thenReturn(existingResponse);

        TaskAssignmentForumResponderGrantResult result =
            service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        assertFalse(result.created());
        assertEquals(existingResponse, result.responder());
        assertEquals(
            ORIGINAL_ADMINISTRATOR_ID,
            result.responder().assignedByUserId());
        assertEquals(
            ASSIGNED_AT,
            result.responder().assignedAt());

        verify(responderRepository, never()).save(any());
        verifyNoInteractions(questionThreadRepository);
    }

    @Test
    void grantResponder_duplicateRace_shouldReturnCurrentRepresentation() {

        ForumResponderCandidate candidate =
            activeOrgCandidate(RESPONDER_ID);

        TaskAssignmentForumResponder persistedAssignment =
            responderAssignment(
                4L,
                RESPONDER_ID,
                ORIGINAL_ADMINISTRATOR_ID,
                ASSIGNED_AT);

        TaskAssignmentForumResponderResponseDTO persistedResponse =
            responderResponse(
                4L,
                RESPONDER_ID,
                ORIGINAL_ADMINISTRATOR_ID,
                ASSIGNED_AT,
                candidate);

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(candidate);

        when(responderRepository.insertIfAbsent(
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(ADMINISTRATOR_ID),
            any(Instant.class)))
            .thenReturn(0);

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.of(persistedAssignment));

        when(responderMapper.toResponse(
            persistedAssignment,
            candidate))
            .thenReturn(persistedResponse);

        TaskAssignmentForumResponderGrantResult result =
            service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        assertFalse(result.created());
        assertEquals(4L, result.responder().id());
        assertEquals(
            ORIGINAL_ADMINISTRATOR_ID,
            result.responder().assignedByUserId());
        assertEquals(
            ASSIGNED_AT,
            result.responder().assignedAt());

        verify(responderRepository, never()).save(any());
    }

    @Test
    void grantResponder_unexpectedInsertedRowCount_shouldThrow() {

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(activeOrgCandidate(RESPONDER_ID));

        when(responderRepository.insertIfAbsent(
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(ADMINISTRATOR_ID),
            any(Instant.class)))
            .thenReturn(2);

        assertThrows(
            IllegalStateException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verify(responderRepository, never())
            .findByTaskAssignmentIdAndResponderUserId(
                any(),
                any());

        verifyNoInteractions(
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void grantResponder_assignmentMissingAfterInsert_shouldThrow() {

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(activeOrgCandidate(RESPONDER_ID));

        when(responderRepository.insertIfAbsent(
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(ADMINISTRATOR_ID),
            any(Instant.class)))
            .thenReturn(1);

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            IllegalStateException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verifyNoInteractions(
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void grantResponder_missingTaskAssignment_shouldReject() {

        prepareAdministrator();

        when(taskAssignmentFacade.findAssignmentById(
            TASK_ASSIGNMENT_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            TaskAssignmentNotFoundException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verifyNoInteractions(
            userFacade,
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void grantResponder_missingUser_shouldReject() {

        prepareAdministrator();
        prepareExistingTaskAssignment();

        when(userFacade.findForumResponderCandidateById(
            RESPONDER_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verify(responderRepository, never())
            .insertIfAbsent(
                any(),
                any(),
                any(),
                any());

        verifyNoInteractions(
            responderMapper,
            questionThreadRepository);
    }

    @ParameterizedTest
    @EnumSource(
        value = Role.class,
        names = {
            "USER",
            "ADMIN",
            "AUTHOR",
            "JURY"
        })
    void grantResponder_nonOrgRole_shouldReject(
        Role role) {

        prepareAdministrator();
        prepareExistingTaskAssignment();

        prepareCandidate(
            candidate(
                RESPONDER_ID,
                "candidate@example.com",
                role,
                UserStatus.ACTIVE));

        assertThrows(
            InvalidForumResponderCandidateException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verify(responderRepository, never())
            .insertIfAbsent(
                any(),
                any(),
                any(),
                any());
    }

    @ParameterizedTest
    @EnumSource(
        value = UserStatus.class,
        names = {
            "PENDING",
            "INACTIVE",
            "BLOCKED",
            "DELETED"
        })
    void grantResponder_nonActiveOrg_shouldReject(
        UserStatus status) {

        prepareAdministrator();
        prepareExistingTaskAssignment();

        prepareCandidate(
            candidate(
                RESPONDER_ID,
                "candidate@example.com",
                Role.ORG,
                status));

        assertThrows(
            InvalidForumResponderCandidateException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verify(responderRepository, never())
            .insertIfAbsent(
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void grantResponder_unauthenticated_shouldRejectBeforeLookups() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verifyNoInteractions(
            taskAssignmentFacade,
            userFacade,
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void grantResponder_nonAdministrator_shouldRejectBeforeLookups() {

        prepareNonAdministrator();

        assertThrows(
            AuthorizationException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verifyNoInteractions(
            taskAssignmentFacade,
            userFacade,
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void grantResponder_invalidTaskAssignmentId_shouldReject() {

        assertThrows(
            ValidationException.class,
            () -> service.grantResponder(
                0L,
                RESPONDER_ID));

        verifyNoInteractions(
            securityFacade,
            taskAssignmentFacade,
            userFacade,
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void grantResponder_invalidResponderId_shouldReject() {

        assertThrows(
            ValidationException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                -1L));

        verifyNoInteractions(
            securityFacade,
            taskAssignmentFacade,
            userFacade,
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    /*
     * Revoke
     */

    @Test
    void revokeResponder_existingUnusedAssignment_shouldDelete() {

        ForumResponderCandidate candidate =
            activeOrgCandidate(RESPONDER_ID);

        TaskAssignmentForumResponder assignment =
            responderAssignment(
                1L,
                RESPONDER_ID,
                ADMINISTRATOR_ID,
                ASSIGNED_AT);

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(candidate);

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.of(assignment));

        when(questionThreadRepository
            .existsByTaskAssignmentIdAndAssignedReviewerIdAndState(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID,
                OPEN))
            .thenReturn(false);

        service.revokeResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);

        verify(responderRepository).delete(assignment);

        verify(questionThreadRepository)
            .existsByTaskAssignmentIdAndAssignedReviewerIdAndState(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID,
                OPEN);
    }

    @Test
    void revokeResponder_inactiveFormerOrg_shouldStillDelete() {

        ForumResponderCandidate inactiveCandidate =
            candidate(
                RESPONDER_ID,
                "former-org@example.com",
                Role.ORG,
                UserStatus.INACTIVE);

        TaskAssignmentForumResponder assignment =
            responderAssignment(
                1L,
                RESPONDER_ID,
                ADMINISTRATOR_ID,
                ASSIGNED_AT);

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(inactiveCandidate);

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.of(assignment));

        when(questionThreadRepository
            .existsByTaskAssignmentIdAndAssignedReviewerIdAndState(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID,
                OPEN))
            .thenReturn(false);

        service.revokeResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);

        verify(responderRepository).delete(assignment);
    }

    @Test
    void revokeResponder_userWhoseRoleChanged_shouldStillDelete() {

        ForumResponderCandidate formerOrgCandidate =
            candidate(
                RESPONDER_ID,
                "former-org@example.com",
                Role.USER,
                UserStatus.ACTIVE);

        TaskAssignmentForumResponder assignment =
            responderAssignment(
                1L,
                RESPONDER_ID,
                ADMINISTRATOR_ID,
                ASSIGNED_AT);

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(formerOrgCandidate);

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.of(assignment));

        when(questionThreadRepository
            .existsByTaskAssignmentIdAndAssignedReviewerIdAndState(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID,
                OPEN))
            .thenReturn(false);

        service.revokeResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);

        verify(responderRepository).delete(assignment);
    }

    @Test
    void revokeResponder_missingUser_shouldReturnNotFound() {

        prepareAdministrator();
        prepareExistingTaskAssignment();

        when(userFacade.findForumResponderCandidateById(
            RESPONDER_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> service.revokeResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verifyNoInteractions(
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void revokeResponder_missingResponderAssignment_shouldBeIdempotent() {

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(activeOrgCandidate(RESPONDER_ID));

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.empty());

        service.revokeResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);

        verifyNoInteractions(questionThreadRepository);

        verify(responderRepository, never())
            .delete(any());
    }

    @Test
    void revokeResponder_repeatedRequest_shouldRemainIdempotent() {

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(activeOrgCandidate(RESPONDER_ID));

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.empty());

        service.revokeResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);

        service.revokeResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);

        verify(userFacade, times(2))
            .findForumResponderCandidateById(
                RESPONDER_ID);

        verify(responderRepository, times(2))
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        verifyNoInteractions(questionThreadRepository);

        verify(responderRepository, never())
            .delete(any());
    }

    @Test
    void revokeResponder_activeReview_shouldRejectAndPreserveData() {

        TaskAssignmentForumResponder assignment =
            responderAssignment(
                1L,
                RESPONDER_ID,
                ADMINISTRATOR_ID,
                ASSIGNED_AT);

        prepareAdministrator();
        prepareExistingTaskAssignment();
        prepareCandidate(activeOrgCandidate(RESPONDER_ID));

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.of(assignment));

        when(questionThreadRepository
            .existsByTaskAssignmentIdAndAssignedReviewerIdAndState(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID,
                OPEN))
            .thenReturn(true);

        assertThrows(
            ForumResponderActiveReviewException.class,
            () -> service.revokeResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verify(responderRepository, never()).delete(any());
        verify(responderRepository, never()).deleteById(any());
        verify(responderRepository, never())
            .deleteByTaskAssignmentIdAndResponderUserId(
                any(),
                any());

        verify(questionThreadRepository, never()).save(any());
        verify(questionThreadRepository, never()).delete(any());
    }

    @Test
    void revokeResponder_missingTaskAssignment_shouldReject() {

        prepareAdministrator();

        when(taskAssignmentFacade.findAssignmentById(
            TASK_ASSIGNMENT_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            TaskAssignmentNotFoundException.class,
            () -> service.revokeResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verifyNoInteractions(
            userFacade,
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void revokeResponder_unauthenticated_shouldRejectBeforeLookups() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> service.revokeResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verifyNoInteractions(
            taskAssignmentFacade,
            userFacade,
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void revokeResponder_nonAdministrator_shouldRejectBeforeLookups() {

        prepareNonAdministrator();

        assertThrows(
            AuthorizationException.class,
            () -> service.revokeResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verifyNoInteractions(
            taskAssignmentFacade,
            userFacade,
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void revokeResponder_invalidTaskAssignmentId_shouldReject() {

        assertThrows(
            ValidationException.class,
            () -> service.revokeResponder(
                null,
                RESPONDER_ID));

        verifyNoInteractions(
            securityFacade,
            taskAssignmentFacade,
            userFacade,
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    @Test
    void revokeResponder_invalidResponderId_shouldReject() {

        assertThrows(
            ValidationException.class,
            () -> service.revokeResponder(
                TASK_ASSIGNMENT_ID,
                0L));

        verifyNoInteractions(
            securityFacade,
            taskAssignmentFacade,
            userFacade,
            responderRepository,
            responderMapper,
            questionThreadRepository);
    }

    /*
     * Listing
     */

    @Test
    void getResponders_shouldReturnMappedPageUsingBulkUserLookup() {

        Sort expectedSort =
            Sort.by(
                Sort.Order.desc("assignedAt"),
                Sort.Order.desc("id"));

        Pageable repositoryPageable =
            PageRequest.of(
                1,
                2,
                expectedSort);

        TaskAssignmentForumResponder firstAssignment =
            responderAssignment(
                2L,
                SECOND_RESPONDER_ID,
                ADMINISTRATOR_ID,
                SECOND_ASSIGNED_AT);

        TaskAssignmentForumResponder secondAssignment =
            responderAssignment(
                1L,
                RESPONDER_ID,
                ADMINISTRATOR_ID,
                ASSIGNED_AT);

        Page<TaskAssignmentForumResponder> repositoryPage =
            new PageImpl<>(
                List.of(
                    firstAssignment,
                    secondAssignment),
                repositoryPageable,
                5);

        ForumResponderCandidate firstCandidate =
            candidate(
                SECOND_RESPONDER_ID,
                "first@example.com",
                Role.ORG,
                UserStatus.ACTIVE);

        ForumResponderCandidate secondCandidate =
            candidate(
                RESPONDER_ID,
                "second@example.com",
                Role.ORG,
                UserStatus.ACTIVE);

        TaskAssignmentForumResponderResponseDTO firstResponse =
            responderResponse(
                2L,
                SECOND_RESPONDER_ID,
                ADMINISTRATOR_ID,
                SECOND_ASSIGNED_AT,
                firstCandidate);

        TaskAssignmentForumResponderResponseDTO secondResponse =
            responderResponse(
                1L,
                RESPONDER_ID,
                ADMINISTRATOR_ID,
                ASSIGNED_AT,
                secondCandidate);

        prepareAdministrator();
        prepareExistingTaskAssignment();

        when(responderRepository.findAllByTaskAssignmentId(
            eq(TASK_ASSIGNMENT_ID),
            any(Pageable.class)))
            .thenReturn(repositoryPage);

        when(userFacade.findForumResponderCandidatesByIds(
            List.of(
                SECOND_RESPONDER_ID,
                RESPONDER_ID)))
            .thenReturn(
                List.of(
                    secondCandidate,
                    firstCandidate));

        when(responderMapper.toResponse(
            firstAssignment,
            firstCandidate))
            .thenReturn(firstResponse);

        when(responderMapper.toResponse(
            secondAssignment,
            secondCandidate))
            .thenReturn(secondResponse);

        Page<TaskAssignmentForumResponderResponseDTO> result =
            service.getResponders(
                TASK_ASSIGNMENT_ID,
                1,
                2);

        assertEquals(
            List.of(
                firstResponse,
                secondResponse),
            result.getContent());

        assertEquals(1, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(5, result.getTotalElements());

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(responderRepository)
            .findAllByTaskAssignmentId(
                eq(TASK_ASSIGNMENT_ID),
                pageableCaptor.capture());

        Pageable capturedPageable =
            pageableCaptor.getValue();

        assertEquals(1, capturedPageable.getPageNumber());
        assertEquals(2, capturedPageable.getPageSize());

        Sort.Order assignedAtOrder =
            capturedPageable
                .getSort()
                .getOrderFor("assignedAt");

        Sort.Order idOrder =
            capturedPageable
                .getSort()
                .getOrderFor("id");

        assertNotNull(assignedAtOrder);
        assertNotNull(idOrder);
        assertEquals(DESC, assignedAtOrder.getDirection());
        assertEquals(DESC, idOrder.getDirection());

        verify(userFacade)
            .findForumResponderCandidatesByIds(
                List.of(
                    SECOND_RESPONDER_ID,
                    RESPONDER_ID));
    }

    @Test
    void getResponders_emptyPage_shouldNotResolveUsers() {

        Pageable pageable =
            PageRequest.of(
                0,
                20,
                Sort.by(
                    Sort.Order.desc("assignedAt"),
                    Sort.Order.desc("id")));

        prepareAdministrator();
        prepareExistingTaskAssignment();

        when(responderRepository.findAllByTaskAssignmentId(
            eq(TASK_ASSIGNMENT_ID),
            any(Pageable.class)))
            .thenReturn(Page.empty(pageable));

        Page<TaskAssignmentForumResponderResponseDTO> result =
            service.getResponders(
                TASK_ASSIGNMENT_ID,
                0,
                20);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(userFacade, never())
            .findForumResponderCandidatesByIds(any());

        verifyNoInteractions(responderMapper);
    }

    @Test
    void getResponders_missingCandidateSummary_shouldThrow() {

        TaskAssignmentForumResponder assignment =
            responderAssignment(
                1L,
                RESPONDER_ID,
                ADMINISTRATOR_ID,
                ASSIGNED_AT);

        Page<TaskAssignmentForumResponder> repositoryPage =
            new PageImpl<>(
                List.of(assignment),
                PageRequest.of(0, 20),
                1);

        prepareAdministrator();
        prepareExistingTaskAssignment();

        when(responderRepository.findAllByTaskAssignmentId(
            eq(TASK_ASSIGNMENT_ID),
            any(Pageable.class)))
            .thenReturn(repositoryPage);

        when(userFacade.findForumResponderCandidatesByIds(
            List.of(RESPONDER_ID)))
            .thenReturn(List.of());

        assertThrows(
            IllegalStateException.class,
            () -> service.getResponders(
                TASK_ASSIGNMENT_ID,
                0,
                20));

        verifyNoInteractions(responderMapper);
    }

    @Test
    void getResponders_missingTaskAssignment_shouldReject() {

        prepareAdministrator();

        when(taskAssignmentFacade.findAssignmentById(
            TASK_ASSIGNMENT_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            TaskAssignmentNotFoundException.class,
            () -> service.getResponders(
                TASK_ASSIGNMENT_ID,
                0,
                20));

        verifyNoInteractions(
            responderRepository,
            responderMapper,
            userFacade,
            questionThreadRepository);
    }

    @Test
    void getResponders_unauthenticated_shouldReject() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> service.getResponders(
                TASK_ASSIGNMENT_ID,
                0,
                20));

        verifyNoInteractions(
            taskAssignmentFacade,
            responderRepository,
            responderMapper,
            userFacade,
            questionThreadRepository);
    }

    @Test
    void getResponders_nonAdministrator_shouldReject() {

        prepareNonAdministrator();

        assertThrows(
            AuthorizationException.class,
            () -> service.getResponders(
                TASK_ASSIGNMENT_ID,
                0,
                20));

        verifyNoInteractions(
            taskAssignmentFacade,
            responderRepository,
            responderMapper,
            userFacade,
            questionThreadRepository);
    }

    @Test
    void getResponders_negativePage_shouldRejectBeforeLookups() {

        assertThrows(
            ValidationException.class,
            () -> service.getResponders(
                TASK_ASSIGNMENT_ID,
                -1,
                20));

        verifyNoInteractions(
            securityFacade,
            taskAssignmentFacade,
            responderRepository,
            responderMapper,
            userFacade,
            questionThreadRepository);
    }

    @Test
    void getResponders_zeroSize_shouldRejectBeforeLookups() {

        assertThrows(
            ValidationException.class,
            () -> service.getResponders(
                TASK_ASSIGNMENT_ID,
                0,
                0));

        verifyNoInteractions(
            securityFacade,
            taskAssignmentFacade,
            responderRepository,
            responderMapper,
            userFacade,
            questionThreadRepository);
    }

    @Test
    void getResponders_invalidTaskAssignmentId_shouldReject() {

        assertThrows(
            ValidationException.class,
            () -> service.getResponders(
                -1L,
                0,
                20));

        verifyNoInteractions(
            securityFacade,
            taskAssignmentFacade,
            responderRepository,
            responderMapper,
            userFacade,
            questionThreadRepository);
    }

    /*
     * Eligibility
     */

    @Test
    void isResponder_exactAssignment_shouldReturnTrue() {

        when(responderRepository
            .existsByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        boolean result =
            service.isResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        assertTrue(result);

        verifyNoInteractions(
            securityFacade,
            taskAssignmentFacade,
            userFacade,
            questionThreadRepository);
    }

    @Test
    void isResponder_otherAssignment_shouldReturnFalse() {

        when(responderRepository
            .existsByTaskAssignmentIdAndResponderUserId(
                OTHER_TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(false);

        boolean result =
            service.isResponder(
                OTHER_TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        assertFalse(result);

        verify(responderRepository)
            .existsByTaskAssignmentIdAndResponderUserId(
                OTHER_TASK_ASSIGNMENT_ID,
                RESPONDER_ID);
    }

    @Test
    void isResponder_globalOrgWithoutAssignment_shouldReturnFalse() {

        when(responderRepository
            .existsByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(false);

        boolean result =
            service.isResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        assertFalse(result);
        verifyNoInteractions(securityFacade);
    }

    @Test
    void requireResponder_existingAssignment_shouldComplete() {

        when(responderRepository
            .existsByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        service.requireResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);

        verify(responderRepository)
            .existsByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);
    }

    @Test
    void requireResponder_missingAssignment_shouldReject() {

        when(responderRepository
            .existsByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(false);

        assertThrows(
            ForumResponderAccessRestrictedException.class,
            () -> service.requireResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));
    }

    @Test
    void findTaskAssignmentIdsByResponder_shouldReturnExactAssignments() {

        List<Long> expected =
            List.of(
                TASK_ASSIGNMENT_ID,
                OTHER_TASK_ASSIGNMENT_ID,
                15L);

        when(responderRepository
            .findTaskAssignmentIdsByResponderUserId(
                RESPONDER_ID))
            .thenReturn(expected);

        List<Long> result =
            service.findTaskAssignmentIdsByResponder(
                RESPONDER_ID);

        assertEquals(expected, result);

        verify(responderRepository)
            .findTaskAssignmentIdsByResponderUserId(
                RESPONDER_ID);
    }

    @Test
    void findTaskAssignmentIdsByResponder_noAssignments_shouldReturnEmpty() {

        when(responderRepository
            .findTaskAssignmentIdsByResponderUserId(
                RESPONDER_ID))
            .thenReturn(List.of());

        List<Long> result =
            service.findTaskAssignmentIdsByResponder(
                RESPONDER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void isResponder_invalidTaskAssignmentId_shouldReject() {

        assertThrows(
            ValidationException.class,
            () -> service.isResponder(
                0L,
                RESPONDER_ID));

        verifyNoInteractions(responderRepository);
    }

    @Test
    void requireResponder_invalidUserId_shouldReject() {

        assertThrows(
            ValidationException.class,
            () -> service.requireResponder(
                TASK_ASSIGNMENT_ID,
                -1L));

        verifyNoInteractions(responderRepository);
    }

    @Test
    void findTaskAssignmentIdsByResponder_invalidUserId_shouldReject() {

        assertThrows(
            ValidationException.class,
            () -> service.findTaskAssignmentIdsByResponder(
                null));

        verifyNoInteractions(responderRepository);
    }

    /*
     * Helpers
     */

    private void prepareAdministrator() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(ADMINISTRATOR_ID));

        when(securityFacade.hasRole("ADMIN"))
            .thenReturn(true);
    }

    private void prepareNonAdministrator() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(RESPONDER_ID));

        when(securityFacade.hasRole("ADMIN"))
            .thenReturn(false);
    }

    private void prepareExistingTaskAssignment() {

        when(taskAssignmentFacade.findAssignmentById(
            TASK_ASSIGNMENT_ID))
            .thenReturn(
                Optional.of(
                    new TaskAssignmentDetailDTO(
                        TASK_ASSIGNMENT_ID,
                        TASK_BODY_ID,
                        TOUR_ID,
                        null,
                        null,
                        null)));
    }

    private void prepareCandidate(
        ForumResponderCandidate candidate) {

        when(userFacade.findForumResponderCandidateById(
            candidate.id()))
            .thenReturn(Optional.of(candidate));
    }

    private ForumResponderCandidate activeOrgCandidate(
        Long userId) {

        return candidate(
            userId,
            "responder-%s@example.com"
                .formatted(userId),
            Role.ORG,
            UserStatus.ACTIVE);
    }

    private ForumResponderCandidate candidate(
        Long userId,
        String email,
        Role role,
        UserStatus status) {

        return new ForumResponderCandidate(
            userId,
            email,
            "Olena",
            "Koval",
            role,
            status);
    }

    private TaskAssignmentForumResponder responderAssignment(
        Long id,
        Long responderUserId,
        Long assignedByUserId,
        Instant assignedAt) {

        return TaskAssignmentForumResponder.builder()
            .id(id)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .responderUserId(responderUserId)
            .assignedByUserId(assignedByUserId)
            .assignedAt(assignedAt)
            .build();
    }

    private TaskAssignmentForumResponderResponseDTO responderResponse(
        Long id,
        Long responderUserId,
        Long assignedByUserId,
        Instant assignedAt,
        ForumResponderCandidate candidate) {

        return new TaskAssignmentForumResponderResponseDTO(
            id,
            TASK_ASSIGNMENT_ID,
            responderUserId,
            candidate.email(),
            candidate.firstName(),
            candidate.lastName(),
            assignedByUserId,
            assignedAt);
    }
}