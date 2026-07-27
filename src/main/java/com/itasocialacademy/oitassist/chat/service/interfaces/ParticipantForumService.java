package com.itasocialacademy.oitassist.chat.service.interfaces;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import org.springframework.data.domain.Page;

public interface ParticipantForumService {
    /**
     * Retrieves a page of questions visible to the current participant in the
     * temporary TaskBody-based forum.
     *
     * <p>
     * The result contains public questions and private questions authored by the
     * current participant. Questions are returned using the forum-defined
     * deterministic ordering.
     * </p>
     *
     * @param taskId identifier of the task whose forum is requested
     * @param page   zero-based page number
     * @param size   number of questions requested per page
     * @return page containing question-thread summaries visible to the current
     *         participant
     * @throws com.itasocialacademy.oitassist.core.exceptions.ValidationException     if
     *                                                                                the
     *                                                                                task
     *                                                                                identifier
     *                                                                                or
     *                                                                                pagination
     *                                                                                parameters
     *                                                                                are
     *                                                                                invalid
     * @throws com.itasocialacademy.oitassist.core.exceptions.AuthenticationException if
     *                                                                                the
     *                                                                                current
     *                                                                                user
     *                                                                                is
     *                                                                                not
     *                                                                                authenticated
     */
    Page<QuestionThreadSummaryResponseDTO> getForumQuestions(
        Long taskId,
        int page,
        int size);

    /**
     * Creates a private question in the temporary TaskBody-based forum.
     *
     * <p>
     * The current authenticated user becomes the author. The task identifier,
     * reviewer, status, state, visibility, version, and audit fields are controlled
     * by the backend and cannot be supplied by the client.
     * </p>
     *
     * @param taskId  identifier of the task in whose forum the question is created
     * @param request validated request containing the question title and content
     * @return the created question thread
     * @throws com.itasocialacademy.oitassist.core.exceptions.ValidationException     if
     *                                                                                the
     *                                                                                task
     *                                                                                identifier
     *                                                                                is
     *                                                                                invalid
     * @throws com.itasocialacademy.oitassist.core.exceptions.AuthenticationException if
     *                                                                                the
     *                                                                                current
     *                                                                                user
     *                                                                                is
     *                                                                                not
     *                                                                                authenticated
     */
    QuestionThreadResponseDTO createQuestion(
        Long taskId,
        CreateQuestionRequestDTO request);
}