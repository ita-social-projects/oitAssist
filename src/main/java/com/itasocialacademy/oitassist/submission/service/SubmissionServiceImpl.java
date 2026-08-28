package com.itasocialacademy.oitassist.submission.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import com.itasocialacademy.oitassist.submission.dao.dto.response.SubmissionResponseDTO;
import com.itasocialacademy.oitassist.submission.dao.model.Submission;
import com.itasocialacademy.oitassist.submission.dao.repository.SubmissionRepository;
import com.itasocialacademy.oitassist.submission.exceptions.NotAParticipantException;
import com.itasocialacademy.oitassist.submission.exceptions.SubmissionNotFoundException;
import com.itasocialacademy.oitassist.submission.exceptions.TourIsNotInProgressException;
import com.itasocialacademy.oitassist.submission.mapper.SubmissionMapper;
import com.itasocialacademy.oitassist.submission.service.interfaces.SubmissionService;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskRequirementsDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import java.time.Instant;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {
    private final SubmissionRepository repository;
    private final SubmissionMapper submissionMapper;
    private final SecurityFacade securityFacade;
    private final FileManagerFacade fileManagerFacade;
    private final TaskAssignmentFacade taskAssignmentFacade;
    private final CompetitionFacade competitionFacade;
    private final ParticipationFacade participationFacade;

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String JURY_ROLE = "JURY";

    @Override
    @Transactional
    public SubmissionResponseDTO createSubmission(String comment, Long taskAssignmentId,
        List<MultipartFile> files) {
        Long userId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to create submissions",
                ErrorCode.ACCESS_DENIED));

        TaskAssignmentDetailDTO taskAssignment = taskAssignmentFacade.findAssignmentById(taskAssignmentId)
            .orElseThrow(() -> new TaskAssignmentNotFoundException(taskAssignmentId));

        Long tourId = taskAssignment.tourId();
        TourDetail tourDetail = competitionFacade.findTourById(tourId)
            .orElseThrow(() -> new TourNotFoundException(tourId));

        if (!Objects.equals(tourDetail.executionStatus(), ExecutionStatus.IN_PROGRESS)) {
            throw new TourIsNotInProgressException();
        }

        if (!participationFacade.isUserAStageParticipant(userId, tourDetail.stageId())) {
            throw new NotAParticipantException();
        }

        List<MultipartFile> filesToUpload = findValidFiles(
            files,
            taskAssignment.requirements().requiredFiles());

        Optional<Submission> found = repository.findBySubmittedByAndTaskAssignmentId(userId, taskAssignmentId);
        Submission entity;
        if (found.isPresent()) {
            entity = found.get();
            fileManagerFacade.detachAllFilesByEntity(RelatedEntityType.SUBMISSION, entity.getId(), userId);
            entity.setComment(comment);
            entity.setSubmittedAt(Instant.now());
        } else {
            entity = Submission.builder()
                .taskAssignmentId(taskAssignmentId)
                .comment(comment)
                .submittedBy(userId)
                .submittedAt(Instant.now())
                .build();
            repository.save(entity);
        }

        List<FileDetailsDTO> uploadedFiles =
            fileManagerFacade.uploadFiles(filesToUpload, RelatedEntityType.SUBMISSION, entity.getId(),
                FileRole.GENERIC);

        return submissionMapper.toResponse(entity, uploadedFiles);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponseDTO getSubmissionBySubmittedByAndTaskAssignmentId(Long submittedBy,
        Long taskAssignmentId) {
        if (!securityFacade.hasRole(JURY_ROLE) && !securityFacade.hasRole(ADMIN_ROLE)) {
            throw new InsufficientPermissionsException();
        }
        Submission submission = repository.findBySubmittedByAndTaskAssignmentId(submittedBy, taskAssignmentId)
            .orElseThrow(() -> new SubmissionNotFoundException(submittedBy, taskAssignmentId));
        List<FileDetailsDTO> files =
            fileManagerFacade.getFilesByEntity(RelatedEntityType.SUBMISSION, submission.getId(),
                Set.of(FileRole.GENERIC));
        return submissionMapper.toResponse(submission, files);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponseDTO getSubmissionById(Long id) {
        if (!securityFacade.hasRole(JURY_ROLE) && !securityFacade.hasRole(ADMIN_ROLE)) {
            throw new InsufficientPermissionsException();
        }
        Submission submission = repository.findById(id).orElseThrow(() -> new SubmissionNotFoundException(id));
        List<FileDetailsDTO> files =
            fileManagerFacade.getFilesByEntity(RelatedEntityType.SUBMISSION, submission.getId(),
                Set.of(FileRole.GENERIC));
        return submissionMapper.toResponse(submission, files);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetail getSubmissionDetailById(Long id) {
        if (!securityFacade.hasRole(JURY_ROLE) && !securityFacade.hasRole(ADMIN_ROLE)) {
            throw new InsufficientPermissionsException();
        }
        Submission submission = repository.findById(id).orElseThrow(() -> new SubmissionNotFoundException(id));
        return submissionMapper.toDetail(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponseDTO getMySubmissionByTaskAssignmentId(Long taskAssignmentId) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to view submissions",
                ErrorCode.ACCESS_DENIED));

        TaskAssignmentDetailDTO taskAssignment = taskAssignmentFacade.findAssignmentById(taskAssignmentId)
            .orElseThrow(() -> new TaskAssignmentNotFoundException(taskAssignmentId));

        TourDetail tourDetail = competitionFacade.findTourById(taskAssignment.tourId())
            .orElseThrow(() -> new TourNotFoundException(taskAssignment.tourId()));

        if (!Objects.equals(tourDetail.executionStatus(), ExecutionStatus.IN_PROGRESS)) {
            throw new TourIsNotInProgressException();
        }

        Submission submission = repository.findBySubmittedByAndTaskAssignmentId(currentUserId, taskAssignmentId)
            .orElseThrow(() -> new SubmissionNotFoundException(currentUserId, taskAssignmentId));

        List<FileDetailsDTO> files =
            fileManagerFacade.getFilesByEntity(RelatedEntityType.SUBMISSION, submission.getId(),
                Set.of(FileRole.GENERIC));
        return submissionMapper.toResponse(submission, files);
    }

    /**
     * Filters the given files to only include the ones that pass the requirements
     * of the task assignment. Only one file can be passed through one requirement.
     *
     * @param files         list of files to filter
     * @param requiredFiles list of file requirements
     * @return list of filtered MultipartFiles
     */
    private List<MultipartFile> findValidFiles(
        List<MultipartFile> files,
        List<TaskRequirementsDTO.RequiredFileDTO> requiredFiles) {
        Set<Integer> matchedRequirements = new HashSet<>();
        List<MultipartFile> validFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            for (int i = 0; i < requiredFiles.size(); i++) {
                if (!matchedRequirements.contains(i)
                    && matchesRequirement(file, requiredFiles.get(i))) {
                    validFiles.add(file);
                    matchedRequirements.add(i);
                    break;
                }
            }
        }

        return validFiles;
    }

    /**
     * Helper method that calls other methods to check all the requirements for a
     * specific file.
     *
     * @param file        file to check
     * @param requirement requirements for the file
     * @return True if all requirements are met. False otherwise
     */
    private boolean matchesRequirement(
        MultipartFile file,
        TaskRequirementsDTO.RequiredFileDTO requirement) {
        return !file.isEmpty()
            && matchesName(file, requirement.namingRule())
            && matchesExtension(file, requirement.allowedExtensions())
            && matchesSize(file, requirement.maxFileSizeMb());
    }

    private boolean matchesSize(
        MultipartFile file,
        Integer maxFileSizeMb) {
        if (maxFileSizeMb == null) {
            return true;
        }

        long maxSizeBytes = maxFileSizeMb * 1024L * 1024L;

        return file.getSize() <= maxSizeBytes;
    }

    private boolean matchesExtension(
        MultipartFile file,
        List<String> allowedExtensions) {
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            return false;
        }

        String filename = file.getOriginalFilename();

        if (filename == null) {
            return false;
        }

        int extensionIndex = filename.lastIndexOf('.');

        if (extensionIndex < 0 || extensionIndex == filename.length() - 1) {
            return false;
        }

        String extension = filename.substring(extensionIndex);

        return allowedExtensions.stream()
            .anyMatch(allowed -> allowed.equalsIgnoreCase(extension));
    }

    private boolean matchesName(
        MultipartFile file,
        String namingRule) {
        String filename = file.getOriginalFilename();

        if (filename == null || namingRule == null || namingRule.isBlank()) {
            return false;
        }

        int extensionIndex = filename.lastIndexOf('.');

        String filenameWithoutExtension = extensionIndex > 0
            ? filename.substring(0, extensionIndex)
            : filename;

        return filenameWithoutExtension.equals(namingRule);
    }
}
