package com.itasocialacademy.oitassist.submission.api;

import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import java.util.Collection;
import java.util.List;

public interface SubmissionFacade {
    SubmissionDetail getSubmissionById(Long id);

    List<SubmissionDetail> getSubmissionsByTaskAssignmentId(Long taskAssignmentId);

    /**
     * Retrieves all submissions made by the given user for any of the given task
     * assignments.
     *
     * @param userId            the id of the user who made the submissions
     * @param taskAssignmentIds the ids of the task assignments to look up; an empty
     *                          collection results in an empty list
     * @return a list of {@link SubmissionDetail}, one per task assignment the user
     *         has submitted; task assignments without a submission produce no
     *         entries
     */
    List<SubmissionDetail> getUserSubmissionsByTaskAssignmentIds(Long userId, Collection<Long> taskAssignmentIds);
}