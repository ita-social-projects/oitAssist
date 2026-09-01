package com.itasocialacademy.oitassist.chat.service.interfaces;

import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderGrantResult;
import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderResponseDTO;
import java.util.List;
import org.springframework.data.domain.Page;

public interface TaskAssignmentForumResponderService {
    /** Grants TaskAssignment-specific responder eligibility. */
    TaskAssignmentForumResponderGrantResult grantResponder(Long taskAssignmentId, Long responderUserId);

    /** Revokes TaskAssignment-specific responder eligibility. */
    void revokeResponder(Long taskAssignmentId, Long responderUserId);

    /** Checks eligibility for one exact TaskAssignment. */
    boolean isResponder(Long taskAssignmentId, Long userId);

    /** Requires eligibility for one exact TaskAssignment. */
    void requireResponder(Long taskAssignmentId, Long userId);

    /**
     * Returns TaskAssignment identifiers for which the user is an eligible
     * responder.
     */
    List<Long> findTaskAssignmentIdsByResponder(Long responderUserId);

    Page<TaskAssignmentForumResponderResponseDTO> getResponders(Long taskAssignmentId, int page, int size);
}