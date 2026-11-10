package com.itasocialacademy.oitassist.taskassignment.service.validation;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.filemanager.access.FileAccessValidator;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.task.api.TaskBodyFacade;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.taskassignment.dao.repository.TaskAssignmentRepository;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Validator that dictates the access rules for files attached to the TASK
 * entity.
 *
 * <p>
 * ADMIN may access every task file and ORG only the files of tasks they own.
 * Any other authenticated user (a participant) may access only
 * {@link FileRole#PROBLEM} and {@link FileRole#REFERENCE} files, and only while
 * the task is visibly assigned to a tour that is
 * {@link ExecutionStatus#IN_PROGRESS} in a stage the user participates in.
 * </p>
 */
@Component
public class TaskFileAccessValidator implements FileAccessValidator {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORG_ROLE = "ORG";
    private static final Set<FileRole> PARTICIPANT_ROLES = Set.of(FileRole.PROBLEM, FileRole.REFERENCE);

    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskBodyFacade taskBodyFacade;
    private final CompetitionFacade competitionFacade;
    private final ParticipationFacade participationFacade;

    /**
     * Creates the validator. {@link TaskBodyFacade} is injected lazily to break the
     * bean cycle {@code FileServiceImpl -> FileAccessValidatorResolver -> this ->
     * TaskBodyFacade -> TaskService -> FileManagerFacade -> FileServiceImpl}.
     */
    public TaskFileAccessValidator(TaskAssignmentRepository taskAssignmentRepository,
        @Lazy TaskBodyFacade taskBodyFacade, CompetitionFacade competitionFacade,
        ParticipationFacade participationFacade) {
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskBodyFacade = taskBodyFacade;
        this.competitionFacade = competitionFacade;
        this.participationFacade = participationFacade;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link RelatedEntityType#TASK}
     */
    @Override
    public RelatedEntityType getEntityType() {
        return RelatedEntityType.TASK;
    }

    /**
     * Checks access without knowing the file role, which is treated as the most
     * restrictive case: participants are denied access.
     *
     * @param taskId  the ID of the task entity
     * @param userId  the ID of the current user, or {@code null} if unauthenticated
     * @param hasRole predicate to check current user roles
     * @return true if access is permitted, false otherwise
     */
    @Override
    public boolean canAccess(Long taskId, Long userId, Predicate<String> hasRole) {
        return canAccess(taskId, null, userId, hasRole);
    }

    /**
     * Checks if the user is authorized to access a file of the specified task.
     *
     * @param taskId   the ID of the task entity
     * @param fileRole the role of the requested file, or {@code null} if unknown
     * @param userId   the ID of the current user, or {@code null} if
     *                 unauthenticated
     * @param hasRole  predicate to check current user roles
     * @return true if access is permitted, false otherwise
     */
    @Override
    public boolean canAccess(Long taskId, FileRole fileRole, Long userId, Predicate<String> hasRole) {
        if (userId == null) {
            return false;
        }
        if (hasRole.test(ADMIN_ROLE)) {
            return true;
        }
        if (hasRole.test(ORG_ROLE) && isOwner(taskId, userId)) {
            return true;
        }
        return fileRole != null
            && PARTICIPANT_ROLES.contains(fileRole)
            && hasActiveVisibleTour(taskId, userId);
    }

    private boolean isOwner(Long taskId, Long userId) {
        return taskBodyFacade.findTaskBodyById(taskId)
            .map(TaskBodyDetail::ownerIds)
            .map(ownerIds -> ownerIds.contains(userId))
            .orElse(false);
    }

    private boolean hasActiveVisibleTour(Long taskId, Long userId) {
        List<Long> tourIds = taskAssignmentRepository.findVisibleTourIdsByTaskBodyId(taskId);
        if (tourIds.isEmpty()) {
            return false;
        }
        return competitionFacade.findToursByIds(tourIds).stream()
            .filter(tour -> tour.executionStatus() == ExecutionStatus.IN_PROGRESS)
            .map(TourDetail::stageId)
            .anyMatch(stageId -> participationFacade.isUserAStageParticipant(userId, stageId));
    }
}
