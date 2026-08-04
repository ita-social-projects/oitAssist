package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderGrantResult;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskAssignmentForumResponderServiceImplTest {

    private static final Long TASK_ASSIGNMENT_ID = 10L;
    private static final Long OTHER_TASK_ASSIGNMENT_ID = 11L;
    private static final Long RESPONDER_ID = 20L;
    private static final Long ADMINISTRATOR_ID = 30L;

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

    @Test
    void grantResponder_activeOrg_shouldCreateAssignment() {

        TaskAssignmentForumResponder assignment =
            responderAssignment();

        TaskAssignmentForumResponderDTO dto =
            responderDto();

        prepareAdministrator();
        prepareTaskAssignment();
        prepareCandidate(activeOrgCandidate());

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

        when(responderMapper.toDto(assignment))
            .thenReturn(dto);

        TaskAssignmentForumResponderGrantResult result =
            service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        assertTrue(result.created());
        assertEquals(dto, result.responder());

        verify(responderRepository).insertIfAbsent(
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(ADMINISTRATOR_ID),
            any(Instant.class));

        verifyNoInteractions(questionThreadRepository);
    }

    @Test
    void grantResponder_existingAssignment_shouldBeIdempotent() {

        TaskAssignmentForumResponder assignment =
            responderAssignment();

        TaskAssignmentForumResponderDTO dto =
            responderDto();

        prepareAdministrator();
        prepareTaskAssignment();
        prepareCandidate(activeOrgCandidate());

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
            .thenReturn(Optional.of(assignment));

        when(responderMapper.toDto(assignment))
            .thenReturn(dto);

        TaskAssignmentForumResponderGrantResult result =
            service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        assertFalse(result.created());
        assertEquals(dto, result.responder());

        verify(responderRepository, never()).save(any());
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

        verifyNoInteractions(userFacade);
        verifyNoInteractions(responderRepository);
    }

    @Test
    void grantResponder_missingUser_shouldReject() {

        prepareAdministrator();
        prepareTaskAssignment();

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
    }

    @Test
    void grantResponder_nonOrgUser_shouldReject() {

        prepareAdministrator();
        prepareTaskAssignment();

        prepareCandidate(
            candidate(
                Role.USER,
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

    @Test
    void grantResponder_inactiveOrg_shouldReject() {

        prepareAdministrator();
        prepareTaskAssignment();

        prepareCandidate(
            candidate(
                Role.ORG,
                UserStatus.INACTIVE));

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
    void grantResponder_blockedOrg_shouldReject() {

        prepareAdministrator();
        prepareTaskAssignment();

        prepareCandidate(
            candidate(
                Role.ORG,
                UserStatus.BLOCKED));

        assertThrows(
            InvalidForumResponderCandidateException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));
    }

    @Test
    void grantResponder_unauthenticated_shouldReject() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verifyNoInteractions(taskAssignmentFacade);
        verifyNoInteractions(userFacade);
        verifyNoInteractions(responderRepository);
    }

    @Test
    void grantResponder_nonAdministrator_shouldReject() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(RESPONDER_ID));

        when(securityFacade.hasRole("ADMIN"))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> service.grantResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verifyNoInteractions(taskAssignmentFacade);
        verifyNoInteractions(userFacade);
        verifyNoInteractions(responderRepository);
    }

    @Test
    void revokeResponder_existingUnusedAssignment_shouldDelete() {

        TaskAssignmentForumResponder assignment =
            responderAssignment();

        prepareAdministrator();
        prepareTaskAssignment();

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
        verifyNoInteractions(userFacade);
    }

    @Test
    void revokeResponder_missingAssignment_shouldBeIdempotent() {

        prepareAdministrator();
        prepareTaskAssignment();

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.empty());

        service.revokeResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);

        verifyNoInteractions(questionThreadRepository);
        verify(responderRepository, never()).delete(any());
    }

    @Test
    void revokeResponder_activeReview_shouldReject() {

        TaskAssignmentForumResponder assignment =
            responderAssignment();

        prepareAdministrator();
        prepareTaskAssignment();

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
    }

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
    }

    @Test
    void requireResponder_existingAssignment_shouldReturnNormally() {

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
    void findAssignmentIds_shouldReturnRepositoryResult() {

        List<Long> expected =
            List.of(10L, 15L, 25L);

        when(responderRepository
            .findTaskAssignmentIdsByResponderUserId(
                RESPONDER_ID))
            .thenReturn(expected);

        List<Long> result =
            service.findTaskAssignmentIdsByResponder(
                RESPONDER_ID);

        assertEquals(expected, result);
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

    private void prepareAdministrator() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(ADMINISTRATOR_ID));

        when(securityFacade.hasRole("ADMIN"))
            .thenReturn(true);
    }

    private void prepareTaskAssignment() {

        when(taskAssignmentFacade.findAssignmentById(
            TASK_ASSIGNMENT_ID))
            .thenReturn(Optional.of(
                org.mockito.Mockito.mock(
                    TaskAssignmentDetailDTO.class)));
    }

    private void prepareCandidate(
        ForumResponderCandidate candidate) {

        when(userFacade.findForumResponderCandidateById(
            RESPONDER_ID))
            .thenReturn(Optional.of(candidate));
    }

    private ForumResponderCandidate activeOrgCandidate() {

        return candidate(
            Role.ORG,
            UserStatus.ACTIVE);
    }

    private ForumResponderCandidate candidate(
        Role role,
        UserStatus status) {

        return new ForumResponderCandidate(
            RESPONDER_ID,
            "responder@example.com",
            "Olena",
            "Koval",
            role,
            status);
    }

    private TaskAssignmentForumResponder responderAssignment() {

        return TaskAssignmentForumResponder.builder()
            .id(1L)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .responderUserId(RESPONDER_ID)
            .assignedByUserId(ADMINISTRATOR_ID)
            .assignedAt(
                Instant.parse(
                    "2026-08-04T12:00:00Z"))
            .build();
    }

    private TaskAssignmentForumResponderDTO responderDto() {

        return new TaskAssignmentForumResponderDTO(
            1L,
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID,
            ADMINISTRATOR_ID,
            Instant.parse(
                "2026-08-04T12:00:00Z"));
    }
}