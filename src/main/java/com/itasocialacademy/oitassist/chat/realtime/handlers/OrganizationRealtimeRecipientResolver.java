package com.itasocialacademy.oitassist.chat.realtime.handlers;

import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves current organizing-committee realtime recipients from
 * TaskAssignment-scoped responder assignments.
 */
@Component
@RequiredArgsConstructor
public class OrganizationRealtimeRecipientResolver {
    private final TaskAssignmentForumResponderRepository responderRepository;

    /**
     * Returns all current responders for one exact TaskAssignment.
     *
     * @param taskAssignmentId TaskAssignment identifier
     * @return sorted and deduplicated responder identifiers
     */
    @Transactional(readOnly = true)
    public List<Long> resolveInboxRecipients(
        Long taskAssignmentId) {
        requirePositiveId(
            taskAssignmentId,
            "Task assignment id");

        return responderRepository
            .findDistinctResponderUserIdsByTaskAssignmentId(
                taskAssignmentId)
            .stream()
            .distinct()
            .toList();
    }

    /**
     * Determines whether one user currently has responder eligibility for the exact
     * TaskAssignment.
     */
    @Transactional(readOnly = true)
    public boolean isOrganizationResponder(
        Long taskAssignmentId,
        Long userId) {
        if (taskAssignmentId == null
            || taskAssignmentId <= 0
            || userId == null
            || userId <= 0) {
            return false;
        }
        return responderRepository
            .existsByTaskAssignmentIdAndResponderUserId(
                taskAssignmentId,
                userId);
    }

    private void requirePositiveId(
        Long identifier,
        String fieldName) {
        if (identifier == null
            || identifier <= 0) {
            throw new IllegalArgumentException(
                "%s must be a positive number"
                    .formatted(fieldName));
        }
    }
}