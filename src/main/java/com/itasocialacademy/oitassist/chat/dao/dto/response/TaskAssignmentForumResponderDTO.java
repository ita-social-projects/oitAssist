package com.itasocialacademy.oitassist.chat.dao.dto.response;

import java.time.Instant;

/**
 * Immutable application representation of a TaskAssignment-specific forum
 * responder assignment.
 */
public record TaskAssignmentForumResponderDTO(
    Long id,
    Long taskAssignmentId,
    Long responderUserId,
    Long assignedByUserId,
    Instant assignedAt) {
}