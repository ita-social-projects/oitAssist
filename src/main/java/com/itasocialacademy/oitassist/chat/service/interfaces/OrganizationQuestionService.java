package com.itasocialacademy.oitassist.chat.service.interfaces;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStateRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStatusRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionVisibilityRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import org.springframework.data.domain.Page;

/**
 * Provides question-review operations for the current authenticated
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

    /**
     * Claims an eligible question for review by the current authenticated
     * organizing-committee responder.
     *
     * @param questionId      question identifier
     * @param expectedVersion expected current question version
     * @return updated immutable question projection
     */
    QuestionThreadResponseDTO claimQuestion(
        Long questionId,
        Long expectedVersion);

    /**
     * Publishes an official answer as the assigned organizing-committee responder.
     *
     * <p>
     * The current user must own the question review and retain responder
     * eligibility for the question's exact TaskAssignment. The question must remain
     * open while the answer is persisted.
     * </p>
     *
     * @param questionId question identifier
     * @param request    validated official-answer content
     * @return created immutable official-answer projection
     */
    QuestionMessageResponseDTO publishOfficialAnswer(
        Long questionId,
        CreateOfficialAnswerRequestDTO request);

    /**
     * Changes visibility of a question assigned to the current responder.
     */
    QuestionThreadResponseDTO updateVisibility(
        Long questionId,
        UpdateQuestionVisibilityRequestDTO request);

    /**
     * Changes review status of a question assigned to the current responder.
     */
    QuestionThreadResponseDTO updateStatus(
        Long questionId,
        UpdateQuestionStatusRequestDTO request);

    /**
     * Changes lifecycle state of a question assigned to the current responder.
     */
    QuestionThreadResponseDTO updateState(
        Long questionId,
        UpdateQuestionStateRequestDTO request);
}