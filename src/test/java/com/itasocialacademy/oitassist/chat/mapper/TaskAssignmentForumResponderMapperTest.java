package com.itasocialacademy.oitassist.chat.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import com.itasocialacademy.oitassist.user.api.dto.ForumResponderCandidate;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TaskAssignmentForumResponderMapperTest {

    private static final Long RESPONDER_ASSIGNMENT_ID = 1L;
    private static final Long TASK_ASSIGNMENT_ID = 10L;
    private static final Long RESPONDER_USER_ID = 20L;
    private static final Long ASSIGNED_BY_USER_ID = 30L;

    private static final Instant ASSIGNED_AT =
        Instant.parse("2026-08-04T12:00:00Z");

    private final TaskAssignmentForumResponderMapper mapper =
        Mappers.getMapper(
            TaskAssignmentForumResponderMapper.class);

    @Test
    void toResponse_shouldCombineAssignmentAndUserSummary() {

        TaskAssignmentForumResponder assignment =
            TaskAssignmentForumResponder.builder()
                .id(RESPONDER_ASSIGNMENT_ID)
                .taskAssignmentId(TASK_ASSIGNMENT_ID)
                .responderUserId(RESPONDER_USER_ID)
                .assignedByUserId(ASSIGNED_BY_USER_ID)
                .assignedAt(ASSIGNED_AT)
                .build();

        ForumResponderCandidate candidate =
            new ForumResponderCandidate(
                RESPONDER_USER_ID,
                "org@example.com",
                "Olena",
                "Koval",
                Role.ORG,
                UserStatus.ACTIVE);

        TaskAssignmentForumResponderResponseDTO result =
            mapper.toResponse(
                assignment,
                candidate);

        assertEquals(
            RESPONDER_ASSIGNMENT_ID,
            result.id());

        assertEquals(
            TASK_ASSIGNMENT_ID,
            result.taskAssignmentId());

        assertEquals(
            RESPONDER_USER_ID,
            result.responderUserId());

        assertEquals(
            "org@example.com",
            result.responderEmail());

        assertEquals(
            "Olena",
            result.responderFirstName());

        assertEquals(
            "Koval",
            result.responderLastName());

        assertEquals(
            ASSIGNED_BY_USER_ID,
            result.assignedByUserId());

        assertEquals(
            ASSIGNED_AT,
            result.assignedAt());
    }

    @Test
    void toResponse_nullCandidateFields_shouldPreserveNullDisplayIdentity() {

        TaskAssignmentForumResponder assignment =
            TaskAssignmentForumResponder.builder()
                .id(RESPONDER_ASSIGNMENT_ID)
                .taskAssignmentId(TASK_ASSIGNMENT_ID)
                .responderUserId(RESPONDER_USER_ID)
                .assignedByUserId(ASSIGNED_BY_USER_ID)
                .assignedAt(ASSIGNED_AT)
                .build();

        ForumResponderCandidate candidate =
            new ForumResponderCandidate(
                RESPONDER_USER_ID,
                null,
                null,
                null,
                Role.ORG,
                UserStatus.ACTIVE);

        TaskAssignmentForumResponderResponseDTO result =
            mapper.toResponse(
                assignment,
                candidate);

        assertEquals(
            RESPONDER_ASSIGNMENT_ID,
            result.id());

        assertEquals(
            TASK_ASSIGNMENT_ID,
            result.taskAssignmentId());

        assertEquals(
            RESPONDER_USER_ID,
            result.responderUserId());

        assertEquals(
            ASSIGNED_BY_USER_ID,
            result.assignedByUserId());

        assertEquals(
            ASSIGNED_AT,
            result.assignedAt());

        assertNull(result.responderEmail());

        assertNull(result.responderFirstName());

        assertNull(result.responderLastName());
    }
}