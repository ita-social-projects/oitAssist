package com.itasocialacademy.oitassist.evaluation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.evaluation.api.dto.EvaluationResponse;
import com.itasocialacademy.oitassist.evaluation.api.dto.FileLink;
import com.itasocialacademy.oitassist.evaluation.api.dto.SubmissionForReview;
import com.itasocialacademy.oitassist.evaluation.api.dto.TaskFileLink;
import com.itasocialacademy.oitassist.evaluation.api.dto.TaskReviewResponse;
import com.itasocialacademy.oitassist.evaluation.dao.model.Evaluation;
import com.itasocialacademy.oitassist.evaluation.dao.repository.EvaluationRepository;
import com.itasocialacademy.oitassist.evaluation.service.interfaces.ReviewService;
import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.submission.api.SubmissionFacade;
import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import com.itasocialacademy.oitassist.task.api.TaskBodyFacade;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private static final int MIN_SCORE = 0;
    private static final String UNKNOWN_EMAIL = "unknown";

    private final EvaluationRepository evaluationRepository;
    private final SubmissionFacade submissionFacade;
    private final TaskAssignmentFacade taskAssignmentFacade;
    private final CompetitionFacade competitionFacade;
    private final SecurityFacade securityFacade;
    private final FileManagerFacade fileManagerFacade;
    private final TaskBodyFacade taskBodyFacade;

    @Override
    @Transactional
    public EvaluationResponse evaluate(Long submissionId, Double score, String comment) {
        Long juryId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException(
                "Not authenticated", ErrorCode.ACCESS_DENIED));
        String juryEmail = securityFacade.getCurrentUserEmail().orElse(UNKNOWN_EMAIL);

        SubmissionDetail submission = submissionFacade.getSubmissionById(submissionId);

        TaskAssignmentDetailDTO assignment =
            taskAssignmentFacade.findAssignmentById(submission.taskAssignmentId())
                .orElseThrow(() -> new ValidationException(
                    "Task assignment not found: " + submission.taskAssignmentId(),
                    ErrorCode.TASK_ASSIGNMENT_NOT_FOUND));

        validateTourInEvaluationPhase(assignment.tourId());
        validateScore(score, assignment.maxPoints());

        Evaluation evaluation = evaluationRepository.findBySubmissionId(submissionId)
            .orElse(null);

        if (evaluation == null) {
            evaluation = Evaluation.builder()
                .submissionId(submissionId)
                .score(score)
                .comment(comment)
                .build();
            log.info("Jury {} ({}) set score {} for submission {}",
                juryId, juryEmail, score, submissionId);
        } else {
            log.info("Jury {} ({}) changed score {} -> {} for submission {}",
                juryId, juryEmail, evaluation.getScore(), score, submissionId);
            evaluation.setScore(score);
            evaluation.setComment(comment);
        }

        Evaluation saved = evaluationRepository.save(evaluation);
        return new EvaluationResponse(saved.getSubmissionId(), saved.getScore(), saved.getComment());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskReviewResponse getTaskForReview(Long taskAssignmentId) {
        Long juryId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException(
                "Not authenticated", ErrorCode.ACCESS_DENIED));
        String juryEmail = securityFacade.getCurrentUserEmail().orElse(UNKNOWN_EMAIL);

        TaskAssignmentDetailDTO assignment = taskAssignmentFacade.findAssignmentById(taskAssignmentId)
            .orElseThrow(() -> new ValidationException(
                "Task assignment not found: " + taskAssignmentId,
                ErrorCode.TASK_ASSIGNMENT_NOT_FOUND));

        String taskTitle = taskBodyFacade.findTaskBodyById(assignment.taskBodyId())
            .map(TaskBodyDetail::title)
            .orElse(null);

        List<TaskFileLink> taskFiles = fileManagerFacade
            .getFilesByEntity(RelatedEntityType.TASK, assignment.taskBodyId(),
                Set.of(FileRole.PROBLEM, FileRole.REFERENCE, FileRole.SOLUTION))
            .stream()
            .map(this::toTaskFileLink)
            .toList();

        List<SubmissionDetail> submissionDetails =
            submissionFacade.getSubmissionsByTaskAssignmentId(taskAssignmentId);

        List<Long> submissionIds = submissionDetails.stream()
            .map(SubmissionDetail::id)
            .toList();

        Map<Long, List<FileDetailsDTO>> filesBySubmission = fileManagerFacade
            .getFilesByEntities(RelatedEntityType.SUBMISSION, submissionIds, Set.of(FileRole.GENERIC));

        Map<Long, Evaluation> evaluations = submissionIds.stream()
            .map(evaluationRepository::findBySubmissionId)
            .flatMap(Optional::stream)
            .collect(Collectors.toMap(Evaluation::getSubmissionId, evaluation -> evaluation));

        List<SubmissionForReview> submissions = submissionDetails.stream()
            .map(detail -> toSubmissionForReview(detail, evaluations, filesBySubmission))
            .toList();

        log.info("Jury {} ({}) requested task assignment {} for review",
            juryId, juryEmail, taskAssignmentId);

        return new TaskReviewResponse(
            taskAssignmentId, taskTitle, assignment.maxPoints(), taskFiles, submissions);
    }

    private SubmissionForReview toSubmissionForReview(
        SubmissionDetail detail,
        Map<Long, Evaluation> evaluations,
        Map<Long, List<FileDetailsDTO>> filesBySubmission) {
        Evaluation evaluation = evaluations.get(detail.id());
        List<FileLink> files = filesBySubmission
            .getOrDefault(detail.id(), List.of())
            .stream()
            .map(this::toFileLink)
            .toList();
        return new SubmissionForReview(
            detail.id(),
            evaluation != null ? evaluation.getScore() : null,
            evaluation != null ? evaluation.getComment() : null,
            files);
    }

    private FileLink toFileLink(FileDetailsDTO file) {
        return new FileLink(file.id(), file.storedFilename(), file.url());
    }

    private TaskFileLink toTaskFileLink(FileDetailsDTO file) {
        return new TaskFileLink(file.id(), file.storedFilename(), file.url(), file.fileRole());
    }

    private void validateTourInEvaluationPhase(Long tourId) {
        ExecutionStatus status = competitionFacade.findTourById(tourId)
            .map(TourDetail::executionStatus)
            .orElseThrow(() -> new TourNotFoundException(tourId));

        if (status != ExecutionStatus.CLOSED) {
            throw new ValidationException(
                "Score can be set or changed only while the tour is in evaluation phase",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateScore(Double score, Integer maxPoints) {
        int max = maxPoints != null ? maxPoints : 0;
        if (score == null || score < MIN_SCORE || score > max) {
            throw new ValidationException(
                "Score must be between %d and %d".formatted(MIN_SCORE, max),
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }
}
