package com.itasocialacademy.oitassist.evaluation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.evaluation.api.dto.EvaluationResponse;
import com.itasocialacademy.oitassist.evaluation.api.dto.TaskReviewResponse;
import com.itasocialacademy.oitassist.evaluation.dao.model.Evaluation;
import com.itasocialacademy.oitassist.evaluation.dao.repository.EvaluationRepository;
import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.submission.api.SubmissionFacade;
import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import com.itasocialacademy.oitassist.task.api.TaskBodyFacade;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    private static final Long SUBMISSION_ID = 100L;
    private static final Long ASSIGNMENT_ID = 10L;
    private static final Long TASK_BODY_ID = 5L;
    private static final Long TOUR_ID = 1L;
    private static final Long JURY_ID = 42L;
    private static final Integer MAX_POINTS = 10;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private SubmissionFacade submissionFacade;

    @Mock
    private TaskAssignmentFacade taskAssignmentFacade;

    @Mock
    private CompetitionFacade competitionFacade;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private FileManagerFacade fileManagerFacade;

    @Mock
    private TaskBodyFacade taskBodyFacade;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private SubmissionDetail submission;
    private TaskAssignmentDetailDTO assignment;

    @BeforeEach
    void setUp() {
        submission = SubmissionDetail.builder()
            .id(SUBMISSION_ID)
            .comment("my solution")
            .submittedAt(Instant.parse("2026-09-01T10:00:00Z"))
            .submittedBy(7L)
            .taskAssignmentId(ASSIGNMENT_ID)
            .build();

        assignment = new TaskAssignmentDetailDTO(
            ASSIGNMENT_ID, TASK_BODY_ID, TOUR_ID, null, MAX_POINTS, null);
    }

    // --- evaluate: happy paths ---

    @Test
    void evaluate_ShouldCreateEvaluation_WhenNoneExists() {
        stubEvaluateChain(ExecutionStatus.CLOSED);
        when(evaluationRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(evaluationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        EvaluationResponse response = reviewService.evaluate(SUBMISSION_ID, 7.0, "good");

        ArgumentCaptor<Evaluation> captor = ArgumentCaptor.forClass(Evaluation.class);
        verify(evaluationRepository).save(captor.capture());

        Evaluation saved = captor.getValue();
        assertAll(
            () -> assertNull(saved.getId(), "A new evaluation must not carry an id"),
            () -> assertEquals(SUBMISSION_ID, saved.getSubmissionId()),
            () -> assertEquals(7.0, saved.getScore()),
            () -> assertEquals("good", saved.getComment()),
            () -> assertEquals(SUBMISSION_ID, response.submissionId()),
            () -> assertEquals(7.0, response.score()),
            () -> assertEquals("good", response.comment()));
    }

    @Test
    void evaluate_ShouldUpdateExistingEvaluation_WhenAlreadyEvaluated() {
        Evaluation existing = Evaluation.builder()
            .id(1L)
            .submissionId(SUBMISSION_ID)
            .score(4.0)
            .comment("old")
            .build();

        stubEvaluateChain(ExecutionStatus.CLOSED);
        when(evaluationRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.of(existing));
        when(evaluationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        EvaluationResponse response = reviewService.evaluate(SUBMISSION_ID, 8.5, "better");

        ArgumentCaptor<Evaluation> captor = ArgumentCaptor.forClass(Evaluation.class);
        verify(evaluationRepository).save(captor.capture());

        Evaluation saved = captor.getValue();
        assertAll(
            () -> assertEquals(1L, saved.getId(), "The existing row must be updated, not duplicated"),
            () -> assertEquals(8.5, saved.getScore()),
            () -> assertEquals("better", saved.getComment()),
            () -> assertEquals(8.5, response.score()));
    }

    @Test
    void evaluate_ShouldAcceptFractionalScore_WhenWithinRange() {
        stubEvaluateChain(ExecutionStatus.CLOSED);
        when(evaluationRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(evaluationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        EvaluationResponse response = reviewService.evaluate(SUBMISSION_ID, 4.25, null);

        assertEquals(4.25, response.score());
    }

    @Test
    void evaluate_ShouldAcceptBoundaryScores_WhenScoreIsZeroOrMax() {
        stubEvaluateChain(ExecutionStatus.CLOSED);
        when(evaluationRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());
        when(evaluationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        assertAll(
            () -> assertEquals(0.0, reviewService.evaluate(SUBMISSION_ID, 0.0, null).score()),
            () -> assertEquals(10.0, reviewService.evaluate(SUBMISSION_ID, 10.0, null).score()));
    }

    // --- evaluate: score validation ---

    @Test
    void evaluate_ShouldThrowValidationException_WhenScoreExceedsMaxPoints() {
        stubEvaluateChain(ExecutionStatus.CLOSED);

        assertThrows(ValidationException.class,
            () -> reviewService.evaluate(SUBMISSION_ID, 10.5, null));

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void evaluate_ShouldThrowValidationException_WhenScoreIsNegative() {
        stubEvaluateChain(ExecutionStatus.CLOSED);

        assertThrows(ValidationException.class,
            () -> reviewService.evaluate(SUBMISSION_ID, -0.5, null));

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void evaluate_ShouldThrowValidationException_WhenScoreIsNull() {
        stubEvaluateChain(ExecutionStatus.CLOSED);

        assertThrows(ValidationException.class,
            () -> reviewService.evaluate(SUBMISSION_ID, null, null));

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void evaluate_ShouldRejectAnyPositiveScore_WhenMaxPointsIsNull() {
        TaskAssignmentDetailDTO withoutMaxPoints = new TaskAssignmentDetailDTO(
            ASSIGNMENT_ID, TASK_BODY_ID, TOUR_ID, null, null, null);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(JURY_ID));
        when(submissionFacade.getSubmissionById(SUBMISSION_ID)).thenReturn(submission);
        when(taskAssignmentFacade.findAssignmentById(ASSIGNMENT_ID))
            .thenReturn(Optional.of(withoutMaxPoints));
        when(competitionFacade.findTourById(TOUR_ID)).thenReturn(Optional.of(tour(ExecutionStatus.CLOSED)));

        assertThrows(ValidationException.class,
            () -> reviewService.evaluate(SUBMISSION_ID, 1.0, null));

        verify(evaluationRepository, never()).save(any());
    }

    // --- evaluate: tour phase ---

    @Test
    void evaluate_ShouldThrowValidationException_WhenTourIsNotInEvaluationPhase() {
        stubEvaluateChain(ExecutionStatus.FINISHED);

        assertThrows(ValidationException.class,
            () -> reviewService.evaluate(SUBMISSION_ID, 5.0, null));

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void evaluate_ShouldThrowTourNotFoundException_WhenTourDoesNotExist() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(JURY_ID));
        when(submissionFacade.getSubmissionById(SUBMISSION_ID)).thenReturn(submission);
        when(taskAssignmentFacade.findAssignmentById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(competitionFacade.findTourById(TOUR_ID)).thenReturn(Optional.empty());

        assertThrows(TourNotFoundException.class,
            () -> reviewService.evaluate(SUBMISSION_ID, 5.0, null));

        verify(evaluationRepository, never()).save(any());
    }

    // --- evaluate: lookups and auth ---

    @Test
    void evaluate_ShouldThrowValidationException_WhenAssignmentNotFound() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(JURY_ID));
        when(submissionFacade.getSubmissionById(SUBMISSION_ID)).thenReturn(submission);
        when(taskAssignmentFacade.findAssignmentById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
            () -> reviewService.evaluate(SUBMISSION_ID, 5.0, null));

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void evaluate_ShouldThrowAuthorizationException_WhenUserIsNotAuthenticated() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthorizationException.class,
            () -> reviewService.evaluate(SUBMISSION_ID, 5.0, null));

        verify(evaluationRepository, never()).save(any());
    }

    // --- getTaskForReview ---

    @Test
    void getTaskForReview_ShouldExposeStoredFilenames_WhenSubmissionHasFiles() {
        FileDetailsDTO submissionFile = new FileDetailsDTO(
            200L, "ivan-solution.docx", "a1b2c3d4.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            1024L, FileRole.GENERIC.name(), "/api/v1/files/200");

        stubTaskForReviewChain(List.of(submission), Map.of(SUBMISSION_ID, List.of(submissionFile)));
        when(evaluationRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());

        TaskReviewResponse response = reviewService.getTaskForReview(ASSIGNMENT_ID);

        assertAll(
            () -> assertEquals(ASSIGNMENT_ID, response.taskAssignmentId()),
            () -> assertEquals(MAX_POINTS, response.maxPoints()),
            () -> assertEquals(1, response.submissions().size()),
            () -> assertEquals("a1b2c3d4.docx",
                response.submissions().getFirst().files().getFirst().storedFilename(),
                "Jury must see the opaque stored filename, not the participant's own"));
    }

    @Test
    void getTaskForReview_ShouldReturnNullScore_WhenSubmissionIsNotEvaluatedYet() {
        stubTaskForReviewChain(List.of(submission), Map.of());
        when(evaluationRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());

        TaskReviewResponse response = reviewService.getTaskForReview(ASSIGNMENT_ID);

        assertAll(
            () -> assertNull(response.submissions().getFirst().score()),
            () -> assertNull(response.submissions().getFirst().comment()));
    }

    @Test
    void getTaskForReview_ShouldReturnStoredScore_WhenSubmissionIsEvaluated() {
        Evaluation evaluation = Evaluation.builder()
            .id(1L)
            .submissionId(SUBMISSION_ID)
            .score(4.5)
            .comment("ok")
            .build();

        stubTaskForReviewChain(List.of(submission), Map.of());
        when(evaluationRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.of(evaluation));

        TaskReviewResponse response = reviewService.getTaskForReview(ASSIGNMENT_ID);

        assertAll(
            () -> assertEquals(4.5, response.submissions().getFirst().score()),
            () -> assertEquals("ok", response.submissions().getFirst().comment()));
    }

    @Test
    void getTaskForReview_ShouldThrowValidationException_WhenAssignmentNotFound() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(JURY_ID));
        when(taskAssignmentFacade.findAssignmentById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> reviewService.getTaskForReview(ASSIGNMENT_ID));
    }

    // --- helpers ---

    private void stubEvaluateChain(ExecutionStatus status) {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(JURY_ID));
        when(submissionFacade.getSubmissionById(SUBMISSION_ID)).thenReturn(submission);
        when(taskAssignmentFacade.findAssignmentById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(competitionFacade.findTourById(TOUR_ID)).thenReturn(Optional.of(tour(status)));
    }

    private void stubTaskForReviewChain(
        List<SubmissionDetail> submissions,
        Map<Long, List<FileDetailsDTO>> filesBySubmission) {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(JURY_ID));
        when(taskAssignmentFacade.findAssignmentById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(taskBodyFacade.findTaskBodyById(TASK_BODY_ID)).thenReturn(Optional.empty());
        when(fileManagerFacade.getFilesByEntity(
            RelatedEntityType.TASK, TASK_BODY_ID,
            Set.of(FileRole.PROBLEM, FileRole.REFERENCE, FileRole.SOLUTION)))
            .thenReturn(List.of());
        when(submissionFacade.getSubmissionsByTaskAssignmentId(ASSIGNMENT_ID)).thenReturn(submissions);
        when(fileManagerFacade.getFilesByEntities(
            eq(RelatedEntityType.SUBMISSION), anyList(), eq(Set.of(FileRole.GENERIC))))
            .thenReturn(filesBySubmission);
    }

    private TourDetail tour(ExecutionStatus status) {
        return TourDetail.builder()
            .id(TOUR_ID)
            .stageId(1L)
            .title("Tour 1")
            .executionStatus(status)
            .build();
    }
}