package com.itasocialacademy.oitassist.chat.service.interfaces;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateCommentRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import org.springframework.data.domain.Page;

public interface ParticipantQuestionService {
    /**
     * Retrieves the complete details of a question visible to the current user.
     *
     * @param questionId identifier of the requested question thread
     * @return complete participant-facing question details
     */
    QuestionThreadResponseDTO getQuestionDetails(
        Long questionId);

    /**
     * Retrieves a page of messages belonging to a question visible to the current
     * user.
     *
     * <p>
     * Messages are returned in deterministic chronological order using
     * {@code createdAt ASC, id ASC}.
     * </p>
     *
     * @param questionId identifier of the requested question thread
     * @param page       zero-based page number
     * @param size       requested page size
     * @return page of participant-facing question messages
     */
    Page<QuestionMessageResponseDTO> getQuestionMessages(
        Long questionId,
        int page,
        int size);

    /**
     * Creates a participant comment for an accessible open question.
     *
     * <p>
     * The authenticated user becomes the message author. The question reference,
     * author, message type, identifier and creation timestamp are controlled by the
     * backend. The persisted message type is always {@code COMMENT}.
     * </p>
     *
     * @param questionId identifier of the question being commented on
     * @param request    validated request containing only comment content
     * @return created participant comment
     */
    QuestionMessageResponseDTO addComment(
        Long questionId,
        CreateCommentRequestDTO request);
}