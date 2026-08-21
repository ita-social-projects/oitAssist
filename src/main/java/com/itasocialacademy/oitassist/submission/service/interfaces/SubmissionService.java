package com.itasocialacademy.oitassist.submission.service.interfaces;

import com.itasocialacademy.oitassist.submission.dao.dto.request.SubmissionCreateRequest;
import com.itasocialacademy.oitassist.submission.dao.dto.response.SubmissionResponseDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;

public interface SubmissionService {
    /**
     * Creates a new submission based on the provided request data.
     *
     * @param submissionCreateRequest the request containing submission details
     * @return the created submission as a {@link SubmissionResponseDTO}
     */
    SubmissionResponseDTO createSubmission(SubmissionCreateRequest submissionCreateRequest);

    /**
     * Retrieves a submission by userId and taskAssignmentId.
     *
     * @param userId           unique identifier of the user who sent the submission
     * @param taskAssignmentId unique identifier of the task assignment which submission was sent for
     * @return {@link SubmissionResponseDTO}
     */
    SubmissionResponseDTO getSubmissionFromUserOnTaskAssignment(Long userId, Long taskAssignmentId);
}
