package com.itasocialacademy.oitassist.chat.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderDTO;
import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TaskAssignmentForumResponderMapperTest {

    private final TaskAssignmentForumResponderMapper mapper =
        Mappers.getMapper(
            TaskAssignmentForumResponderMapper.class);

    @Test
    void toDto_shouldMapAllAssignmentFields() {

        Instant assignedAt =
            Instant.parse("2026-08-04T12:00:00Z");

        TaskAssignmentForumResponder entity =
            TaskAssignmentForumResponder.builder()
                .id(1L)
                .taskAssignmentId(10L)
                .responderUserId(20L)
                .assignedByUserId(30L)
                .assignedAt(assignedAt)
                .build();

        TaskAssignmentForumResponderDTO result =
            mapper.toDto(entity);

        assertEquals(1L, result.id());
        assertEquals(10L, result.taskAssignmentId());
        assertEquals(20L, result.responderUserId());
        assertEquals(30L, result.assignedByUserId());
        assertEquals(assignedAt, result.assignedAt());
    }
}