package com.itasocialacademy.oitassist.chat.service.interfaces;

import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderGrantResult;
import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderResponseDTO;
import org.springframework.data.domain.Page;
import java.util.List;

public interface TaskAssignmentForumResponderService {
    /**
     * Grants TaskAssignment-specific responder eligibility.
     *
     * <p>
     * The operation is idempotent. Repeated calls return the existing assignment
     * with {@code created = false}.
     * </p>
     */
    TaskAssignmentForumResponderGrantResult grantResponder(
        Long taskAssignmentId,
        Long responderUserId);

    /**
     * Revokes TaskAssignment-specific responder eligibility.
     *
     * <p>
     * The operation is idempotent when the assignment does not exist. Revocation is
     * rejected when the responder owns an active open review.
     * </p>
     */
    void revokeResponder(
        Long taskAssignmentId,
        Long responderUserId);

    /**
     * Checks eligibility for one exact TaskAssignment.
     */
    boolean isResponder(
        Long taskAssignmentId,
        Long userId);

    /**
     * Requires eligibility for one exact TaskAssignment.
     */
    void requireResponder(
        Long taskAssignmentId,
        Long userId);

    /**
     * Returns TaskAssignment identifiers for which the user is an eligible
     * responder.
     */
    List<Long> findTaskAssignmentIdsByResponder(
        Long responderUserId);

    Page<TaskAssignmentForumResponderResponseDTO> getResponders(
        Long taskAssignmentId,
        int page,
        int size);
}