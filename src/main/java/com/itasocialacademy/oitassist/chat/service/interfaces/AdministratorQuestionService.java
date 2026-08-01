package com.itasocialacademy.oitassist.chat.service.interfaces;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import org.springframework.data.domain.Page;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;

public interface AdministratorQuestionService {
    /**
     * Retrieves open, new and unassigned questions available for review.
     *
     * @param page zero-based page number
     * @param size requested page size
     * @return page of unclaimed administrator inbox items
     */
    Page<AdminQuestionInboxItemResponseDTO> getUnclaimedQuestions(
        int page,
        int size);

    /**
     * Retrieves open questions assigned to the current administrator.
     *
     * @param status optional question-status filter
     * @param page   zero-based page number
     * @param size   requested page size
     * @return page of questions assigned to the current administrator
     */
    Page<AdminQuestionInboxItemResponseDTO> getAssignedQuestions(
        QuestionStatus status,
        int page,
        int size);

    /**
     * Claims an eligible question for review by the current administrator.
     *
     * @param questionId      question identifier
     * @param expectedVersion expected current question version
     * @return updated question
     */
    QuestionThreadResponseDTO claimQuestion(
        Long questionId,
        Long expectedVersion);

    /**
     * Publishes an official answer in an open question thread.
     *
     * <p>
     * The authenticated global administrator becomes the message author. The
     * backend controls the author, question reference, message type, identifier and
     * creation timestamp. The persisted type is always {@code OFFICIAL_ANSWER}.
     * </p>
     *
     * <p>
     * Questions in NEW or IN_REVIEW status transition to ANSWERED. Additional
     * answers to an ANSWERED question preserve that status.
     * </p>
     *
     * @param questionId identifier of the question being answered
     * @param request    validated request containing only answer content
     * @return created official answer
     */
    QuestionMessageResponseDTO publishOfficialAnswer(
        Long questionId,
        CreateOfficialAnswerRequestDTO request);
}