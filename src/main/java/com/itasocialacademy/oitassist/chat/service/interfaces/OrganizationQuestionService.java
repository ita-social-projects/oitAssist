package com.itasocialacademy.oitassist.chat.service.interfaces;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import org.springframework.data.domain.Page;

/**
 * Provides question-review queues for the current authenticated
 * organizing-committee responder.
 */
public interface OrganizationQuestionService {
    /**
     * Returns unclaimed questions from TaskAssignments for which the current ORG
     * user has responder eligibility.
     *
     * @param page zero-based page number
     * @param size requested page size
     * @return responder-scoped unclaimed question page
     */
    Page<QuestionReviewInboxItemResponseDTO> getResponderInbox(
        int page,
        int size);

    /**
     * Returns open questions currently assigned to the authenticated ORG user.
     *
     * @param status optional question-status filter
     * @param page   zero-based page number
     * @param size   requested page size
     * @return current responder's assigned-question page
     */
    Page<QuestionReviewInboxItemResponseDTO> getAssignedToCurrentResponder(
        QuestionStatus status,
        int page,
        int size);
}