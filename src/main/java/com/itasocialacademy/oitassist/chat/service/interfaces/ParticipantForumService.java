package com.itasocialacademy.oitassist.chat.service.interfaces;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionCreationNotAllowedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionForumAccessRestrictedException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import org.springframework.data.domain.Page;

public interface ParticipantForumService {
    /**
     * Retrieves a page of questions visible to the current user in the forum of the
     * specified task assignment.
     *
     * <p>
     * For a participant, the result contains public questions and private questions
     * authored by that participant. Participant access requires a visible task
     * assignment and a matching participation for the assignment's competition and
     * stage.
     * </p>
     *
     * <p>
     * A global administrator may access an existing task assignment forum without a
     * participant record.
     * </p>
     *
     * <p>
     * Questions are returned using the forum-defined deterministic ordering.
     * </p>
     *
     * @param taskAssignmentId identifier of the task assignment whose forum is
     *                         requested
     * @param page             zero-based page number
     * @param size             number of questions requested per page requested
     * @return page containing question-thread summaries visible to the current user
     * @throws ValidationException                    if the task assignment
     *                                                identifier or pagination
     *                                                parameters are invalid
     * @throws AuthenticationException                if the current user is not
     *                                                authenticated
     * @throws TaskAssignmentNotFoundException        if the task assignment does
     *                                                not exist
     * @throws TourNotFoundException                  if the task assignment
     *                                                references a missing tour
     * @throws StageNotFoundException                 if the related tour references
     *                                                a missing stage
     * @throws QuestionForumAccessRestrictedException if the task assignment is
     *                                                hidden or matching
     *                                                participation is missing
     */
    Page<QuestionThreadSummaryResponseDTO> getForumQuestions(Long taskAssignmentId, int page, int size);

    /**
     * Creates a private question in the forum of the specified task assignment.
     *
     * <p>
     * For a participant, the task assignment must be visible and matching
     * participation must exist for the assignment's competition and stage. The
     * related tour must have the {@code IN_PROGRESS} execution status.
     * </p>
     *
     * <p>
     * The authenticated user becomes the author. The task assignment identifier,
     * reviewer, status, state, visibility, version, and audit fields are controlled
     * by the backend and cannot be supplied by the client.
     * </p>
     *
     * @param taskAssignmentId identifier of the task assignment in whose forum the
     *                         question is created
     * @param request          validated request containing the question title and
     *                         content
     * @return created question thread
     * @throws ValidationException                    if the task assignment
     *                                                identifier is invalid
     * @throws AuthenticationException                if the current user is not
     *                                                authenticated
     * @throws TaskAssignmentNotFoundException        if the task assignment does
     *                                                not exist
     * @throws TourNotFoundException                  if the task assignment
     *                                                references a missing tour
     * @throws StageNotFoundException                 if the related tour references
     *                                                a missing stage
     * @throws QuestionForumAccessRestrictedException if the task assignment is
     *                                                hidden or matching
     *                                                participation is missing
     * @throws QuestionCreationNotAllowedException    if the related tour is not in
     *                                                progress
     */
    QuestionThreadResponseDTO createQuestion(Long taskAssignmentId, CreateQuestionRequestDTO request);
}