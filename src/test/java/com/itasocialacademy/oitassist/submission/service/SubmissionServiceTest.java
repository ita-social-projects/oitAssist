package com.itasocialacademy.oitassist.submission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.submission.dao.dto.response.SubmissionResponseDTO;
import com.itasocialacademy.oitassist.submission.dao.model.Submission;
import com.itasocialacademy.oitassist.submission.dao.repository.SubmissionRepository;
import com.itasocialacademy.oitassist.submission.exceptions.NotAParticipantException;
import com.itasocialacademy.oitassist.submission.exceptions.SubmissionNotFoundException;
import com.itasocialacademy.oitassist.submission.exceptions.TourIsNotInProgressException;
import com.itasocialacademy.oitassist.submission.mapper.SubmissionMapper;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskRequirementsDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class SubmissionServiceTest {

    @Mock
    private SecurityFacade securityFacade;
    @Mock
    private FileManagerFacade fileManagerFacade;
    @Mock
    private TaskAssignmentFacade taskAssignmentFacade;
    @Mock
    private SubmissionRepository repository;
    @InjectMocks
    private SubmissionServiceImpl submissionService;
    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private CompetitionFacade competitionFacade;
    @Mock
    private ParticipationFacade participationFacade;

    private Submission submission;
    private SubmissionResponseDTO response;
    private List<FileDetailsDTO> files;

    @BeforeEach
    void setUp() {
        submission = Submission.builder()
            .id(1L)
            .submittedBy(100L)
            .taskAssignmentId(200L)
            .comment("Test comment")
            .build();

        files = List.of(
            new FileDetailsDTO(10L, "file1.cpp", "document",
                1024L, "GENERIC", "/uploads/file1.cpp"));

        response = SubmissionResponseDTO.builder()
            .id(1L)
            .submittedBy(100L)
            .taskAssignmentId(200L)
            .comment("Test comment")
            .build();
    }

    @Test
    @DisplayName("getSubmissionById should return submission when user is admin")
    void getSubmissionById_ShouldReturnSubmission_WhenUserIsAdmin() {
        Long submissionId = 1L;

        when(securityFacade.hasRole("JURY")).thenReturn(false);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(fileManagerFacade.getFilesByEntity(
            eq(RelatedEntityType.SUBMISSION),
            eq(submissionId),
            eq(Set.of(FileRole.GENERIC)))).thenReturn(files);
        when(submissionMapper.toResponse(submission, files)).thenReturn(response);

        SubmissionResponseDTO result = submissionService.getSubmissionById(submissionId);

        assertThat(result).isSameAs(response);

        verify(securityFacade).hasRole("JURY");
        verify(securityFacade).hasRole("ADMIN");
        verify(repository).findById(submissionId);
        verify(fileManagerFacade).getFilesByEntity(
            RelatedEntityType.SUBMISSION,
            submissionId,
            Set.of(FileRole.GENERIC));
        verify(submissionMapper).toResponse(submission, files);
    }

    @Test
    @DisplayName("getSubmissionById should throw InsufficientPermissionsException when user is not jury or admin")
    void getSubmissionById_ShouldThrowInsufficientPermissionsException_WhenUserIsNotJuryOrAdmin() {
        Long submissionId = 1L;

        when(securityFacade.hasRole("JURY")).thenReturn(false);
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        assertThatThrownBy(() -> submissionService.getSubmissionById(submissionId))
            .isInstanceOf(InsufficientPermissionsException.class);

        verify(securityFacade).hasRole("JURY");
        verify(securityFacade).hasRole("ADMIN");
        verifyNoInteractions(repository, fileManagerFacade, submissionMapper);
    }

    @Test
    @DisplayName("getSubmissionById should throw SubmissionNotFoundException when submission does not exist")
    void getSubmissionById_ShouldThrowSubmissionNotFoundException_WhenSubmissionDoesNotExist() {
        Long submissionId = 1L;

        when(securityFacade.hasRole("JURY")).thenReturn(false);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(repository.findById(submissionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getSubmissionById(submissionId))
            .isInstanceOf(SubmissionNotFoundException.class);

        verify(repository).findById(submissionId);
        verifyNoInteractions(fileManagerFacade, submissionMapper);
    }

    @Test
    @DisplayName("getSubmissionBySubmittedByAndTaskAssignmentId should return submission when user is jury")
    void getSubmissionBySubmittedByAndTaskAssignmentId_ShouldReturnSubmission_WhenUserIsJury() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;

        when(securityFacade.hasRole("JURY")).thenReturn(true);
        when(repository.findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId))
            .thenReturn(Optional.of(submission));
        when(fileManagerFacade.getFilesByEntity(
            eq(RelatedEntityType.SUBMISSION),
            eq(1L),
            eq(Set.of(FileRole.GENERIC)))).thenReturn(files);
        when(submissionMapper.toResponse(submission, files)).thenReturn(response);

        SubmissionResponseDTO result =
            submissionService.getSubmissionBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId);

        assertThat(result).isSameAs(response);

        verify(securityFacade).hasRole("JURY");
        verify(repository).findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId);
        verify(fileManagerFacade).getFilesByEntity(
            RelatedEntityType.SUBMISSION,
            1L,
            Set.of(FileRole.GENERIC));
        verify(submissionMapper).toResponse(submission, files);
    }

    @Test
    @DisplayName("getSubmissionBySubmittedByAndTaskAssignmentId should throw InsufficientPermissionsException when " +
        "user is not jury or admin")
    void getSubmissionBySubmittedByAndTaskAssignmentId_ShouldThrowInsufficientPermissionsException_WhenUserIsNotJuryOrAdmin() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;

        when(securityFacade.hasRole("JURY")).thenReturn(false);
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        assertThatThrownBy(
            () -> submissionService.getSubmissionBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId))
            .isInstanceOf(InsufficientPermissionsException.class);

        verifyNoInteractions(repository, fileManagerFacade, submissionMapper);
    }

    @Test
    @DisplayName("getSubmissionBySubmittedByAndTaskAssignmentId should throw SubmissionNotFoundException when " +
        "submission does not exist")
    void getSubmissionBySubmittedByAndTaskAssignmentId_ShouldThrowSubmissionNotFoundException_WhenSubmissionDoesNotExist() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;

        when(securityFacade.hasRole("JURY")).thenReturn(false);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(repository.findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> submissionService.getSubmissionBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId))
            .isInstanceOf(SubmissionNotFoundException.class);

        verify(repository).findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId);
        verifyNoInteractions(fileManagerFacade, submissionMapper);
    }

    @Test
    @DisplayName("getMySubmissionByTaskAssignmentId should return submission for current user")
    void getMySubmissionByTaskAssignmentId_ShouldReturnSubmission_WhenSubmissionExists() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;
        Long tourId = 300L;

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));

        TaskAssignmentDetailDTO assignment = mock(TaskAssignmentDetailDTO.class);
        when(assignment.tourId()).thenReturn(tourId);

        TourDetail tourDetail = mock(TourDetail.class);
        when(tourDetail.executionStatus()).thenReturn(ExecutionStatus.IN_PROGRESS);

        when(taskAssignmentFacade.findAssignmentById(taskAssignmentId))
            .thenReturn(Optional.of(assignment));

        when(competitionFacade.findTourById(tourId))
            .thenReturn(Optional.of(tourDetail));

        when(repository.findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId))
            .thenReturn(Optional.of(submission));

        when(fileManagerFacade.getFilesByEntity(
            eq(RelatedEntityType.SUBMISSION),
            eq(1L),
            eq(Set.of(FileRole.GENERIC))))
            .thenReturn(files);

        when(submissionMapper.toResponse(submission, files))
            .thenReturn(response);

        SubmissionResponseDTO result =
            submissionService.getMySubmissionByTaskAssignmentId(taskAssignmentId);

        assertThat(result).isSameAs(response);

        verify(securityFacade).getCurrentUserId();

        verify(taskAssignmentFacade).findAssignmentById(taskAssignmentId);

        verify(competitionFacade).findTourById(tourId);

        verify(repository).findBySubmittedByAndTaskAssignmentId(
            userId,
            taskAssignmentId);

        verify(fileManagerFacade).getFilesByEntity(
            RelatedEntityType.SUBMISSION,
            1L,
            Set.of(FileRole.GENERIC));

        verify(submissionMapper).toResponse(submission, files);
    }

    @Test
    @DisplayName("getMySubmissionByTaskAssignmentId should throw AuthorizationException when user is not authenticated")
    void getMySubmissionByTaskAssignmentId_ShouldThrowAuthorizationException_WhenUserIsNotAuthenticated() {
        Long taskAssignmentId = 200L;

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getMySubmissionByTaskAssignmentId(taskAssignmentId))
            .isInstanceOf(AuthorizationException.class);

        verify(securityFacade).getCurrentUserId();
        verifyNoInteractions(repository, fileManagerFacade, submissionMapper);
    }

    @Test
    @DisplayName("getMySubmissionByTaskAssignmentId should throw SubmissionNotFoundException when submission does not exist")
    void getMySubmissionByTaskAssignmentId_ShouldThrowSubmissionNotFoundException_WhenSubmissionDoesNotExist() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;
        Long tourId = 300L;

        TaskAssignmentDetailDTO assignment = mock(TaskAssignmentDetailDTO.class);
        when(assignment.tourId()).thenReturn(tourId);

        TourDetail tourDetail = mock(TourDetail.class);
        when(tourDetail.executionStatus()).thenReturn(ExecutionStatus.IN_PROGRESS);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));

        when(taskAssignmentFacade.findAssignmentById(taskAssignmentId))
            .thenReturn(Optional.of(assignment));

        when(competitionFacade.findTourById(tourId))
            .thenReturn(Optional.of(tourDetail));

        when(repository.findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getMySubmissionByTaskAssignmentId(taskAssignmentId))
            .isInstanceOf(SubmissionNotFoundException.class);

        verify(securityFacade).getCurrentUserId();

        verify(taskAssignmentFacade).findAssignmentById(taskAssignmentId);

        verify(competitionFacade).findTourById(tourId);

        verify(repository).findBySubmittedByAndTaskAssignmentId(
            userId,
            taskAssignmentId);

        verifyNoInteractions(
            fileManagerFacade,
            submissionMapper);
    }

    private TaskRequirementsDTO taskRequirements() {
        return new TaskRequirementsDTO(List.of(
            new TaskRequirementsDTO.RequiredFileDTO(
                "file1",
                List.of(".cpp", ".java"),
                10),
            new TaskRequirementsDTO.RequiredFileDTO(
                "report",
                List.of(".pdf"),
                5)));
    }

    @Test
    @DisplayName("createSubmission should create submission and upload valid files")
    void createSubmission_ShouldCreateSubmissionAndUploadValidFiles() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;
        Long tourId = 300L;
        Long stageId = 400L;

        MultipartFile validFile = mock(MultipartFile.class);
        MultipartFile invalidFile = mock(MultipartFile.class);

        when(validFile.isEmpty()).thenReturn(false);
        doReturn("file1.cpp").when(validFile).getOriginalFilename();
        when(validFile.getSize()).thenReturn(1024L);

        when(invalidFile.isEmpty()).thenReturn(false);
        doReturn("unknown.cpp").when(invalidFile).getOriginalFilename();

        TaskRequirementsDTO requirements = taskRequirements();

        TaskAssignmentDetailDTO assignment = mock(TaskAssignmentDetailDTO.class);
        when(assignment.tourId()).thenReturn(tourId);
        when(assignment.requirements()).thenReturn(requirements);

        TourDetail tourDetail = mock(TourDetail.class);
        when(tourDetail.executionStatus()).thenReturn(ExecutionStatus.IN_PROGRESS);
        when(tourDetail.stageId()).thenReturn(stageId);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));

        when(taskAssignmentFacade.findAssignmentById(taskAssignmentId))
            .thenReturn(Optional.of(assignment));

        when(competitionFacade.findTourById(tourId))
            .thenReturn(Optional.of(tourDetail));

        when(participationFacade.isUserAStageParticipant(userId, stageId))
            .thenReturn(true);

        when(repository.findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId))
            .thenReturn(Optional.empty());

        when(repository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        when(fileManagerFacade.uploadFiles(
            eq(List.of(validFile)),
            eq(RelatedEntityType.SUBMISSION),
            eq(1L),
            eq(FileRole.GENERIC)))
            .thenReturn(files);

        when(submissionMapper.toResponse(any(Submission.class), eq(files)))
            .thenReturn(response);

        SubmissionResponseDTO result = submissionService.createSubmission(
            "Test comment",
            taskAssignmentId,
            List.of(validFile, invalidFile));

        assertThat(result).isSameAs(response);

        verify(securityFacade).getCurrentUserId();

        verify(taskAssignmentFacade).findAssignmentById(taskAssignmentId);

        verify(competitionFacade).findTourById(tourId);

        verify(participationFacade).isUserAStageParticipant(userId, stageId);

        verify(repository).findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId);
        verify(repository).save(any(Submission.class));

        verify(fileManagerFacade).uploadFiles(
            eq(List.of(validFile)),
            eq(RelatedEntityType.SUBMISSION),
            eq(1L),
            eq(FileRole.GENERIC));

        verify(submissionMapper).toResponse(
            any(Submission.class),
            eq(files));
    }

    @Test
    @DisplayName("createSubmission should update existing submission and replace its files")
    void createSubmission_ShouldUpdateExistingSubmission_WhenSubmissionAlreadyExists() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;
        Long tourId = 300L;
        Long stageId = 400L;

        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        doReturn("file1.cpp").when(file).getOriginalFilename();
        when(file.getSize()).thenReturn(1024L);

        TaskRequirementsDTO requirements = taskRequirements();

        TaskAssignmentDetailDTO assignment = mock(TaskAssignmentDetailDTO.class);
        when(assignment.tourId()).thenReturn(tourId);
        when(assignment.requirements()).thenReturn(requirements);

        TourDetail tourDetail = mock(TourDetail.class);
        when(tourDetail.executionStatus()).thenReturn(ExecutionStatus.IN_PROGRESS);
        when(tourDetail.stageId()).thenReturn(stageId);

        Submission existingSubmission = Submission.builder()
            .id(1L)
            .submittedBy(userId)
            .taskAssignmentId(taskAssignmentId)
            .comment("Old comment")
            .build();

        List<FileDetailsDTO> uploadedFiles = List.of();

        SubmissionResponseDTO response = SubmissionResponseDTO.builder()
            .id(1L)
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));

        when(taskAssignmentFacade.findAssignmentById(taskAssignmentId))
            .thenReturn(Optional.of(assignment));

        when(competitionFacade.findTourById(tourId))
            .thenReturn(Optional.of(tourDetail));

        when(participationFacade.isUserAStageParticipant(userId, stageId))
            .thenReturn(true);

        when(repository.findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId))
            .thenReturn(Optional.of(existingSubmission));

        when(fileManagerFacade.uploadFiles(
            eq(List.of(file)),
            eq(RelatedEntityType.SUBMISSION),
            eq(1L),
            eq(FileRole.GENERIC)))
            .thenReturn(uploadedFiles);

        when(submissionMapper.toResponse(existingSubmission, uploadedFiles))
            .thenReturn(response);

        SubmissionResponseDTO result = submissionService.createSubmission(
            "New comment",
            taskAssignmentId,
            List.of(file));

        assertThat(result).isSameAs(response);
        assertThat(existingSubmission.getComment()).isEqualTo("New comment");

        verify(securityFacade).getCurrentUserId();

        verify(taskAssignmentFacade).findAssignmentById(taskAssignmentId);

        verify(competitionFacade).findTourById(tourId);

        verify(participationFacade).isUserAStageParticipant(userId, stageId);

        verify(repository).findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId);
        verify(repository, never()).save(any(Submission.class));

        verify(fileManagerFacade).detachAllFilesByEntity(
            RelatedEntityType.SUBMISSION,
            1L,
            userId);

        verify(fileManagerFacade).uploadFiles(
            eq(List.of(file)),
            eq(RelatedEntityType.SUBMISSION),
            eq(1L),
            eq(FileRole.GENERIC));

        verify(submissionMapper).toResponse(existingSubmission, uploadedFiles);
    }

    @Test
    @DisplayName("createSubmission should throw TaskAssignmentNotFoundException when assignment does not exist")
    void createSubmission_ShouldThrowTaskAssignmentNotFoundException_WhenAssignmentDoesNotExist() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));

        when(taskAssignmentFacade.findAssignmentById(taskAssignmentId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.createSubmission(
            "Test comment",
            taskAssignmentId,
            List.of(mock(MultipartFile.class))))
            .isInstanceOf(TaskAssignmentNotFoundException.class);

        verify(securityFacade).getCurrentUserId();

        verify(taskAssignmentFacade).findAssignmentById(taskAssignmentId);

        verifyNoInteractions(
            competitionFacade,
            participationFacade,
            repository,
            fileManagerFacade,
            submissionMapper);
    }

    @Test
    @DisplayName("createSubmission should upload only one file when multiple files match the same requirement")
    void createSubmission_ShouldUploadOnlyOneMatchingFile_WhenFilesMatchSameRequirement() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;
        Long tourId = 1L;
        Long stageId = 200L;

        MultipartFile firstFile = mock(MultipartFile.class);
        MultipartFile secondFile = mock(MultipartFile.class);

        when(firstFile.isEmpty()).thenReturn(false);
        doReturn("file1.cpp").when(firstFile).getOriginalFilename();
        when(firstFile.getSize()).thenReturn(1024L);

        TaskRequirementsDTO requirements = new TaskRequirementsDTO(List.of(
            new TaskRequirementsDTO.RequiredFileDTO(
                "file1",
                List.of(".cpp", ".java"),
                10)));

        TaskAssignmentDetailDTO assignment = mock(TaskAssignmentDetailDTO.class);
        when(assignment.tourId()).thenReturn(tourId);
        when(assignment.requirements()).thenReturn(requirements);

        TourDetail tourDetail = mock(TourDetail.class);
        when(tourDetail.executionStatus()).thenReturn(ExecutionStatus.IN_PROGRESS);
        when(tourDetail.stageId()).thenReturn(stageId);

        when(competitionFacade.findTourById(tourId))
            .thenReturn(Optional.of(tourDetail));
        when(tourDetail.executionStatus())
            .thenReturn(ExecutionStatus.IN_PROGRESS);
        when(tourDetail.stageId())
            .thenReturn(stageId);

        when(participationFacade.isUserAStageParticipant(userId, stageId))
            .thenReturn(true);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(taskAssignmentFacade.findAssignmentById(taskAssignmentId))
            .thenReturn(Optional.of(assignment));

        when(repository.findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId))
            .thenReturn(Optional.empty());

        when(repository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        List<FileDetailsDTO> uploadedFiles = List.of();

        when(fileManagerFacade.uploadFiles(
            anyList(),
            eq(RelatedEntityType.SUBMISSION),
            eq(1L),
            eq(FileRole.GENERIC))).thenReturn(uploadedFiles);

        SubmissionResponseDTO response = SubmissionResponseDTO.builder()
            .id(1L)
            .build();

        when(submissionMapper.toResponse(
            any(Submission.class),
            eq(uploadedFiles))).thenReturn(response);

        SubmissionResponseDTO result = submissionService.createSubmission(
            "Test",
            taskAssignmentId,
            List.of(firstFile, secondFile));

        assertThat(result).isSameAs(response);

        verify(fileManagerFacade).uploadFiles(
            eq(List.of(firstFile)),
            eq(RelatedEntityType.SUBMISSION),
            eq(1L),
            eq(FileRole.GENERIC));

        verify(submissionMapper).toResponse(
            any(Submission.class),
            eq(uploadedFiles));
    }

    @Test
    @DisplayName("createSubmission should throw TourIsNotInProgressException when tour is not in progress")
    void createSubmission_ShouldThrowTourIsNotInProgressException_WhenTourIsNotInProgress() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;
        Long tourId = 300L;

        TaskAssignmentDetailDTO assignment = mock(TaskAssignmentDetailDTO.class);
        when(assignment.tourId()).thenReturn(tourId);

        TourDetail tourDetail = mock(TourDetail.class);
        when(tourDetail.executionStatus()).thenReturn(ExecutionStatus.SCHEDULED);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(taskAssignmentFacade.findAssignmentById(taskAssignmentId))
            .thenReturn(Optional.of(assignment));
        when(competitionFacade.findTourById(tourId))
            .thenReturn(Optional.of(tourDetail));

        assertThatThrownBy(() -> submissionService.createSubmission(
            "Test comment",
            taskAssignmentId,
            List.of()))
            .isInstanceOf(TourIsNotInProgressException.class);

        verify(securityFacade).getCurrentUserId();
        verify(taskAssignmentFacade).findAssignmentById(taskAssignmentId);
        verify(competitionFacade).findTourById(tourId);

        verifyNoInteractions(
            participationFacade,
            repository,
            fileManagerFacade,
            submissionMapper);
    }

    @Test
    @DisplayName("createSubmission should throw NotAParticipantException when user is not a stage participant")
    void createSubmission_ShouldThrowNotAParticipantException_WhenUserIsNotStageParticipant() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;
        Long tourId = 300L;
        Long stageId = 400L;

        TaskAssignmentDetailDTO assignment = mock(TaskAssignmentDetailDTO.class);
        when(assignment.tourId()).thenReturn(tourId);

        TourDetail tourDetail = mock(TourDetail.class);
        when(tourDetail.executionStatus()).thenReturn(ExecutionStatus.IN_PROGRESS);
        when(tourDetail.stageId()).thenReturn(stageId);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(taskAssignmentFacade.findAssignmentById(taskAssignmentId))
            .thenReturn(Optional.of(assignment));
        when(competitionFacade.findTourById(tourId))
            .thenReturn(Optional.of(tourDetail));
        when(participationFacade.isUserAStageParticipant(userId, stageId))
            .thenReturn(false);

        assertThatThrownBy(() -> submissionService.createSubmission(
            "Test comment",
            taskAssignmentId,
            List.of()))
            .isInstanceOf(NotAParticipantException.class);

        verify(securityFacade).getCurrentUserId();
        verify(taskAssignmentFacade).findAssignmentById(taskAssignmentId);
        verify(competitionFacade).findTourById(tourId);
        verify(participationFacade).isUserAStageParticipant(userId, stageId);

        verifyNoInteractions(
            repository,
            fileManagerFacade,
            submissionMapper);
    }

    @Test
    @DisplayName("getMySubmissionByTaskAssignmentId should throw TaskAssignmentNotFoundException when assignment does not exist")
    void getMySubmissionByTaskAssignmentId_ShouldThrowTaskAssignmentNotFoundException_WhenAssignmentDoesNotExist() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(taskAssignmentFacade.findAssignmentById(taskAssignmentId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getMySubmissionByTaskAssignmentId(taskAssignmentId))
            .isInstanceOf(TaskAssignmentNotFoundException.class);

        verify(securityFacade).getCurrentUserId();
        verify(taskAssignmentFacade).findAssignmentById(taskAssignmentId);

        verifyNoInteractions(
            competitionFacade,
            repository,
            fileManagerFacade,
            submissionMapper);
    }

    @Test
    @DisplayName("getMySubmissionByTaskAssignmentId should throw TourIsNotInProgressException when tour is not in progress")
    void getMySubmissionByTaskAssignmentId_ShouldThrowTourIsNotInProgressException_WhenTourIsNotInProgress() {
        Long userId = 100L;
        Long taskAssignmentId = 200L;
        Long tourId = 300L;

        TaskAssignmentDetailDTO assignment = mock(TaskAssignmentDetailDTO.class);
        when(assignment.tourId()).thenReturn(tourId);

        TourDetail tourDetail = mock(TourDetail.class);
        when(tourDetail.executionStatus()).thenReturn(ExecutionStatus.FINISHED);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(taskAssignmentFacade.findAssignmentById(taskAssignmentId))
            .thenReturn(Optional.of(assignment));
        when(competitionFacade.findTourById(tourId))
            .thenReturn(Optional.of(tourDetail));

        assertThatThrownBy(() -> submissionService.getMySubmissionByTaskAssignmentId(taskAssignmentId))
            .isInstanceOf(TourIsNotInProgressException.class);

        verify(securityFacade).getCurrentUserId();
        verify(taskAssignmentFacade).findAssignmentById(taskAssignmentId);
        verify(competitionFacade).findTourById(tourId);

        verifyNoInteractions(
            repository,
            fileManagerFacade,
            submissionMapper);
    }
}
