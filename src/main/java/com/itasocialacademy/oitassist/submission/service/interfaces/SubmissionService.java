package com.itasocialacademy.oitassist.submission.service.interfaces;

import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import com.itasocialacademy.oitassist.submission.dao.dto.response.SubmissionResponseDTO;
import com.itasocialacademy.oitassist.submission.exceptions.SubmissionNotFoundException;
import com.itasocialacademy.oitassist.submission.exceptions.TourIsNotInProgressException;
import com.itasocialacademy.oitassist.submission.exceptions.NotAParticipantException;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface SubmissionService {
    /**
     * Creates a new submission based on the provided request data.
     *
     * @param comment          optional comment for the submission
     * @param taskAssignmentId the ID of the task assignment for this submission
     * @param files            files to submit
     * @return the created submission as a {@link SubmissionResponseDTO}
     * @throws AuthorizationException          if current user is unauthorized
     * @throws TaskAssignmentNotFoundException if task assignment is not found by
     *                                         the given id
     * @throws TourNotFoundException           if tour is not found
     * @throws TourIsNotInProgressException    if tour is not currently in progress
     * @throws NotAParticipantException        if current user is not a tour
     *                                         participant
     */
    SubmissionResponseDTO createSubmission(String comment, Long taskAssignmentId, List<MultipartFile> files);

    /**
     * Retrieves a submission by userId and taskAssignmentId. Only users with ADMIN
     * or JURY role can perform this action
     *
     * @param submittedBy      unique identifier of the user who sent the submission
     * @param taskAssignmentId unique identifier of the task assignment which
     *                         submission was sent for
     * @return {@link SubmissionResponseDTO}
     * @throws SubmissionNotFoundException      if there is no submission found with
     *                                          given userId and taskAssignmentId
     * @throws InsufficientPermissionsException if user doesnt have ADMIN or JURY
     *                                          role
     */
    SubmissionResponseDTO getSubmissionBySubmittedByAndTaskAssignmentId(Long submittedBy, Long taskAssignmentId);

    /**
     * Retrieves a submission by id. Only users with ADMIN or JURY role can perform
     * this action
     *
     * @param id unique identifier of the submission
     * @return {@link SubmissionResponseDTO}
     * @throws SubmissionNotFoundException      if there is no submission found with
     *                                          given id
     * @throws InsufficientPermissionsException if user doesnt have ADMIN or JURY
     *                                          role
     */
    SubmissionResponseDTO getSubmissionById(Long id);

    /**
     * Retrieves a submission detail by id. Only users with ADMIN or JURY role can
     * perform this action
     *
     * @param id unique identifier of the submission
     * @return {@link SubmissionDetail}
     * @throws SubmissionNotFoundException      if there is no submission found with
     *                                          given id
     * @throws InsufficientPermissionsException if user doesnt have ADMIN or JURY
     *                                          role
     */
    SubmissionDetail getSubmissionDetailById(Long id);

    /**
     * Retrieves a current user`s submission by task assignment id.
     *
     * @param taskAssignmentId unique identifier of the task assignment which
     *                         submission was sent for
     * @return {@link SubmissionResponseDTO}
     * @throws AuthorizationException          if current user is unauthorized
     * @throws SubmissionNotFoundException     if there is no submission found with
     *                                         currentUserId and taskAssignmentId
     * @throws TaskAssignmentNotFoundException if task assignment is not found by
     *                                         the given id
     * @throws TourNotFoundException           if tour is not found
     * @throws TourIsNotInProgressException    if tour is not currently in progress
     */
    SubmissionResponseDTO getMySubmissionByTaskAssignmentId(Long taskAssignmentId);
}
