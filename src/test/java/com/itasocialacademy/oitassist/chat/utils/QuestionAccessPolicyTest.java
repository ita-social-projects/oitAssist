package com.itasocialacademy.oitassist.chat.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.task.api.TaskBodyFacade;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionAccessPolicyTest {

    private static final Long TASK_ID = 1L;
    private static final Long USER_ID = 100L;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private TaskBodyFacade taskBodyFacade;

    @InjectMocks
    private QuestionAccessPolicy questionAccessPolicy;

    @Test
    void requireTaskForumAccess_existingTask_shouldReturnCurrentUserId() {
        TaskBodyDetail taskBodyDetail = createTaskBodyDetail();

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(USER_ID));
        when(taskBodyFacade.findTaskBodyById(TASK_ID))
            .thenReturn(Optional.of(taskBodyDetail));

        Long result =
            questionAccessPolicy.requireTaskForumAccess(TASK_ID);

        assertEquals(USER_ID, result);

        verify(securityFacade).getCurrentUserId();
        verify(taskBodyFacade).findTaskBodyById(TASK_ID);
    }

    @Test
    void requireTaskForumAccess_unauthenticated_shouldThrowAuthenticationException() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> questionAccessPolicy.requireTaskForumAccess(TASK_ID));

        verify(securityFacade).getCurrentUserId();
        verifyNoInteractions(taskBodyFacade);
    }

    @Test
    void requireTaskForumAccess_missingTask_shouldThrowTaskNotFoundException() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(USER_ID));
        when(taskBodyFacade.findTaskBodyById(TASK_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            TaskNotFoundException.class,
            () -> questionAccessPolicy.requireTaskForumAccess(TASK_ID));

        verify(securityFacade).getCurrentUserId();
        verify(taskBodyFacade).findTaskBodyById(TASK_ID);
    }

    @Test
    void hasTaskAccess_existingTaskAndAuthenticatedUser_shouldReturnTrue() {
        TaskBodyDetail taskBodyDetail = createTaskBodyDetail();

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(USER_ID));
        when(taskBodyFacade.findTaskBodyById(TASK_ID))
            .thenReturn(Optional.of(taskBodyDetail));

        boolean result =
            questionAccessPolicy.hasTaskAccess(TASK_ID);

        assertTrue(result);

        verify(securityFacade).getCurrentUserId();
        verify(taskBodyFacade).findTaskBodyById(TASK_ID);
    }

    @Test
    void hasTaskAccess_missingTask_shouldReturnFalse() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(USER_ID));
        when(taskBodyFacade.findTaskBodyById(TASK_ID))
            .thenReturn(Optional.empty());

        boolean result =
            questionAccessPolicy.hasTaskAccess(TASK_ID);

        assertFalse(result);

        verify(securityFacade).getCurrentUserId();
        verify(taskBodyFacade).findTaskBodyById(TASK_ID);
    }

    @Test
    void hasTaskAccess_unauthenticatedUser_shouldReturnFalse() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        boolean result =
            questionAccessPolicy.hasTaskAccess(TASK_ID);

        assertFalse(result);

        verify(securityFacade).getCurrentUserId();
        verifyNoInteractions(taskBodyFacade);
    }

    @Test
    void hasTaskAccess_invalidTaskId_shouldReturnFalse() {
        assertFalse(questionAccessPolicy.hasTaskAccess((Long) null));
        assertFalse(questionAccessPolicy.hasTaskAccess(0L));
        assertFalse(questionAccessPolicy.hasTaskAccess(-1L));

        verifyNoInteractions(securityFacade, taskBodyFacade);
    }

    private TaskBodyDetail createTaskBodyDetail() {
        return new TaskBodyDetail(
            TASK_ID,
            "Test task",
            "Test task description",
            Set.of(USER_ID));
    }
}