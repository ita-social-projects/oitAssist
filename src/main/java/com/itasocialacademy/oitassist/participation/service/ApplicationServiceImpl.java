package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.participation.components.saver.ApplicationDecisionsSaver;
import com.itasocialacademy.oitassist.participation.dao.dto.event.ApplicationDecisionListEvent;
import com.itasocialacademy.oitassist.participation.dao.dto.event.ApplicationDecisionEvent;
import com.itasocialacademy.oitassist.participation.dao.dto.request.AcceptApplicationListRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectApplicationListRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.*;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import com.itasocialacademy.oitassist.participation.dao.repository.ApplicationRepository;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import com.itasocialacademy.oitassist.participation.dao.specification.ApplicationSpecification;
import com.itasocialacademy.oitassist.participation.exceptions.ApplicationNotFoundException;
import com.itasocialacademy.oitassist.participation.exceptions.UnableToProcessApplicationException;
import com.itasocialacademy.oitassist.participation.exceptions.UnexpectedConstraintViolationException;
import com.itasocialacademy.oitassist.participation.exceptions.UserApplicationRequestException;
import com.itasocialacademy.oitassist.participation.mapper.UserEnrollmentAssembler;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ApplicationMapper;
import com.itasocialacademy.oitassist.participation.mapper.ParticipationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ProcessApplicationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.UserSummaryMapper;
import com.itasocialacademy.oitassist.participation.components.scheduler.AfterCommitScheduler;
import com.itasocialacademy.oitassist.participation.components.sender.AsyncEmailSender;
import com.itasocialacademy.oitassist.participation.service.interfaces.ApplicationService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {
    private static final RequestStatus PENDING_STATUS = RequestStatus.PENDING;
    private static final String UNIQUE_PARTICIPATION_CONSTRAINT = "uc_participants_competition_id_stage_id";

    private final ParticipationRepository participationRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final SecurityFacade securityFacade;
    private final ParticipationMapper participationMapper;
    private final ProcessApplicationMapper processApplicationMapper;
    private final CompetitionFacade competitionFacade;
    private final AsyncEmailSender emailSender;
    private final UserFacade userFacade;
    private final AfterCommitScheduler scheduler;
    private final UserEnrollmentAssembler enrollmentAssembler;
    private final UserSummaryMapper userSummaryMapper;
    private final ApplicationDecisionsSaver applicationSaver;

    @Override
    @Transactional
    public CreateApplicationResponse sendApplicationRequest(Long competitionId, Long stageId) {
        Long userId = getCurrentUserIdOrThrow();
        validateUserCanApply(userId, competitionId, stageId);
        Application application = Application.builder()
            .competitionId(competitionId)
            .stageId(stageId)
            .status(PENDING_STATUS)
            .build();
        return applicationMapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional
    public ProcessApplicationResponse acceptRequest(Long applicationId) {
        Application application = getPendingApplicationOrThrow(applicationId);
        Long userId = getCurrentUserIdOrThrow();
        participationRepository.save(participationMapper.toParticipation(application));
        application.setStatus(RequestStatus.ACCEPTED);
        application.setProcessedBy(userId);
        application.setProcessedAt(Instant.now());
        ProcessApplicationResponse response = processApplicationMapper.toResponse(
            applicationRepository.saveAndFlush(application));

        scheduleAcceptedEmail(
            application.getCompetitionId(),
            application.getStageId(),
            application.getIssuedBy());

        return response;
    }

    @Override
    public AcceptedApplicationListResponse acceptApplicationList(AcceptApplicationListRequest request) {
        Long userId = getCurrentUserIdOrThrow();
        List<Long> applicationIds = validateNoDuplicatesOrThrow(request.applicationIds());

        List<Application> applications = applicationRepository.findAll(
            ApplicationSpecification.applicationIdIn(applicationIds));
        validateApplicationsBelongToTheSameCompetitionAndStage(applications);
        Long competitionId = applications.getFirst().getCompetitionId();
        Long stageId = applications.getFirst().getStageId();

        List<SucceededApplicationAcceptingItemResponse> succeeded = new ArrayList<>();
        List<FailedApplicationDecisionItemResponse> failed = new ArrayList<>();

        Set<Long> foundIds = applications.stream().map(Application::getId).collect(Collectors.toSet());
        applicationIds.stream()
            .filter(id -> !foundIds.contains(id))
            .forEach(id -> failed.add(new FailedApplicationDecisionItemResponse(id, "Application not found")));

        for (Application application : applications) {
            if (application.getStatus() != PENDING_STATUS) {
                failed.add(new FailedApplicationDecisionItemResponse(
                    application.getId(), "Application is not pending"));
                continue;
            }
            try {
                Participation savedParticipation = applicationSaver.saveAcceptedApplicationData(
                    userId, application, competitionId, stageId);
                succeeded.add(new SucceededApplicationAcceptingItemResponse(
                    application.getId(), savedParticipation.getUserId(), RequestStatus.ACCEPTED));
            } catch (DataIntegrityViolationException e) {
                if (isUniqueParticipationConstraint(e)) {
                    failed.add(new FailedApplicationDecisionItemResponse(
                        application.getId(), "Application already has a participation record"));
                } else {
                    throw new UnexpectedConstraintViolationException(
                        "Unexpected database constraint violation while accepting application",
                        ErrorCode.DATA_ACCESS_ERROR, e);
                }
            }
        }
        AcceptedApplicationListResponse response = AcceptedApplicationListResponse.builder()
            .application(new ApplicationDecisionSummary(competitionId, stageId, userId, Instant.now()))
            .succeeded(succeeded)
            .failed(failed)
            .build();

        if (!succeeded.isEmpty()) {
            scheduleAcceptedEmailList(
                competitionId,
                stageId,
                succeeded.stream().map(SucceededApplicationAcceptingItemResponse::participantId).toList());
        }

        return response;
    }

    @Override
    @Transactional
    public ProcessApplicationResponse rejectRequest(Long applicationId, RejectEnrollmentRequest request) {
        Application application = getPendingApplicationOrThrow(applicationId);
        Long userId = getCurrentUserIdOrThrow();
        application.setStatus(RequestStatus.REJECTED);
        application.setRejectionReason(request.rejectionReason());
        application.setProcessedBy(userId);
        application.setProcessedAt(Instant.now());
        ProcessApplicationResponse response =
            processApplicationMapper.toResponse(applicationRepository.saveAndFlush(application));

        scheduleRejectedEmail(
            application.getCompetitionId(),
            application.getStageId(),
            application.getIssuedBy(),
            application.getRejectionReason());

        return response;
    }

    @Override
    public RejectedApplicationListResponse rejectApplicationList(RejectApplicationListRequest request) {
        Long userId = getCurrentUserIdOrThrow();
        List<Long> applicationIds = validateNoDuplicatesOrThrow(request.applicationIds());

        List<Application> applications = applicationRepository.findAll(
            ApplicationSpecification.applicationIdIn(applicationIds));
        validateApplicationsBelongToTheSameCompetitionAndStage(applications);
        Long competitionId = applications.getFirst().getCompetitionId();
        Long stageId = applications.getFirst().getStageId();

        List<SucceededApplicationRejectingItemResponse> succeeded = new ArrayList<>();
        List<FailedApplicationDecisionItemResponse> failed = new ArrayList<>();

        Set<Long> foundIds = applications.stream().map(Application::getId).collect(Collectors.toSet());
        applicationIds.stream()
            .filter(id -> !foundIds.contains(id))
            .forEach(id -> failed.add(new FailedApplicationDecisionItemResponse(id, "Application not found")));

        for (Application application : applications) {
            if (application.getStatus() != PENDING_STATUS) {
                failed.add(new FailedApplicationDecisionItemResponse(
                    application.getId(), "Application is not pending"));
                continue;
            }
            try {
                Application savedApplication = applicationSaver.saveRejectedApplication(
                    userId, application, request.rejectionReason());
                succeeded.add(new SucceededApplicationRejectingItemResponse(
                    savedApplication.getId(), savedApplication.getUserId(), RequestStatus.REJECTED));
            } catch (DataIntegrityViolationException e) {
                throw new UnexpectedConstraintViolationException(
                    "Unexpected database constraint violation while accepting application",
                    ErrorCode.DATA_ACCESS_ERROR, e);
            }
        }
        RejectedApplicationListResponse response = RejectedApplicationListResponse.builder()
            .application(new ApplicationDecisionSummary(competitionId, stageId, userId, Instant.now()))
            .succeeded(succeeded)
            .failed(failed)
            .build();

        if (!succeeded.isEmpty()) {
            scheduleRejectedEmailList(
                competitionId,
                stageId,
                succeeded.stream().map(SucceededApplicationRejectingItemResponse::studentId).toList(),
                request.rejectionReason());
        }

        return response;
    }

    @Override
    @Transactional
    public ProcessApplicationResponse cancelRequest(Long applicationId) {
        Long userId = getCurrentUserIdOrThrow();
        Application application = getPendingApplicationOrThrow(applicationId);
        validateUserCanCancelApplication(userId, application);
        application.setStatus(RequestStatus.CANCELLED);
        application.setProcessedBy(userId);
        application.setProcessedAt(Instant.now());
        return processApplicationMapper.toResponse(applicationRepository.saveAndFlush(application));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationListItemResponse> getEnrollmentRequests(
        Long competitionId,
        Long stageId,
        String search,
        Pageable pageable) {
        validateCompetitionAndStageInfo(competitionId, stageId);
        List<Long> candidateUserIds = applicationRepository.findAll(
            ApplicationSpecification.hasCompetitionAndStage(competitionId, stageId)
                .and(ApplicationSpecification.hasStatus(PENDING_STATUS)))
            .stream()
            .map(Application::getUserId)
            .distinct()
            .toList();
        if (candidateUserIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Optional<List<Long>> matchingUserIds = userFacade.findUserIdsBySearchWithinIds(search, candidateUserIds);
        List<Long> filterIds = matchingUserIds.orElse(candidateUserIds);

        if (matchingUserIds.isPresent() && matchingUserIds.get().isEmpty()) {
            return Page.empty(pageable);
        }
        Page<Application> applications = applicationRepository.findAll(
            ApplicationSpecification.hasCompetitionAndStage(competitionId, stageId)
                .and(ApplicationSpecification.userIdIn(filterIds))
                .and(ApplicationSpecification.hasStatus(PENDING_STATUS)),
            pageable);
        List<ApplicationListItemResponse> responses = enrollmentAssembler.enrichWithUser(
            applications.toList(), Application::getUserId,
            (application, user) -> new ApplicationListItemResponse(
                application.getId(),
                application.getIssuedAt(),
                application.getStatus(),
                userSummaryMapper.toUserSummary(user)));
        return new PageImpl<>(responses, pageable, applications.getTotalElements());
    }

    private void validateUserCanApply(Long userId, Long competitionId, Long stageId) {
        validateNoPendingApplication(userId, competitionId, stageId);
        validateUserDoesNotAlreadyParticipate(userId, competitionId, stageId);
        validateCompetitionAndStageInfoForApplying(competitionId, stageId);
    }

    private void validateNoPendingApplication(Long userId, Long competitionId, Long stageId) {
        boolean hasPendingApplication = applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            userId,
            competitionId,
            stageId,
            PENDING_STATUS);
        if (hasPendingApplication) {
            throw new UserApplicationRequestException("User already has a pending request");
        }
    }

    private void validateUserDoesNotAlreadyParticipate(Long userId, Long competitionId, Long stageId) {
        boolean isParticipant = participationRepository.existsByUserIdAndCompetitionIdAndStageId(
            userId,
            competitionId,
            stageId);
        if (isParticipant) {
            throw new UserApplicationRequestException("User is already a participant");
        }
    }

    private void validateCompetitionAndStageInfoForApplying(Long competitionId, Long stageId) {
        CompetitionDetail competitionDetail = getCompetitionInfoOrThrow(competitionId);
        StageDetail stageDetail = getStageInfoOrThrow(stageId);
        validateHierarchy(competitionId, stageDetail);
        if (competitionDetail.competitionStatus() != CompetitionStatus.ENROLLMENT) {
            throw new UserApplicationRequestException("The competition cannot be enrolled");
        }
        if (stageDetail.scope() != StageScope.DISTRICT && stageDetail.scope() != StageScope.CITY) {
            throw new UserApplicationRequestException("The specified stage cannot be enrolled");
        }
    }

    private void validateCompetitionAndStageInfo(Long competitionId, Long stageId) {
        getCompetitionInfoOrThrow(competitionId);
        StageDetail stageDetail = getStageInfoOrThrow(stageId);
        validateHierarchy(competitionId, stageDetail);
    }

    private void validateHierarchy(Long competitionId, StageDetail stageDetail) {
        if (!stageDetail.competitionId().equals(competitionId)) {
            throw new CompetitionHierarchyValidationException("Specified stage does not belong to this competition");
        }
    }

    private Application getPendingApplicationOrThrow(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ApplicationNotFoundException("The application was not found"));
        if (application.getStatus() != PENDING_STATUS) {
            throw new UnableToProcessApplicationException("The application request is not in the PENDING status");
        }
        return application;
    }

    private void validateUserCanCancelApplication(Long userId, Application application) {
        if (!userId.equals(application.getIssuedBy())) {
            throw new UnableToProcessApplicationException("The application does not belong to the current user");
        }
    }

    private Long getCurrentUserIdOrThrow() {
        return securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User is not authenticated",
                ErrorCode.ACCESS_DENIED));
    }

    private void scheduleDecisionEmailAfterCommit(
        Long competitionId, Long stageId, Long userId, String rejectionReason, RequestStatus status) {
        String competitionTitle = getCompetitionInfoOrThrow(competitionId).title();
        String stageTitle = getStageInfoOrThrow(stageId).title();
        UserProfileDetails user = getUserOrThrow(userId);

        ApplicationDecisionEvent event = new ApplicationDecisionEvent(
            competitionTitle, stageTitle, user.firstName(), user.email(), rejectionReason, status);
        scheduler.runAfterCommit(() -> emailSender.sendDecisionEmail(event));
    }

    private CompetitionDetail getCompetitionInfoOrThrow(Long competitionId) {
        return competitionFacade.findCompetitionById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));
    }

    private StageDetail getStageInfoOrThrow(Long stageId) {
        return competitionFacade.findStageById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));
    }

    private UserProfileDetails getUserOrThrow(Long userId) {
        return userFacade.findProfileById(userId)
            .orElseThrow(UserNotFoundException::new);
    }

    private void scheduleAcceptedEmail(Long competitionId, Long stageId, Long userId) {
        scheduleDecisionEmailAfterCommit(competitionId, stageId, userId, null, RequestStatus.ACCEPTED);
    }

    private void scheduleRejectedEmail(Long competitionId, Long stageId, Long userId, String rejectionReason) {
        scheduleDecisionEmailAfterCommit(competitionId, stageId, userId, rejectionReason, RequestStatus.REJECTED);
    }

    private void scheduleAcceptedEmailList(Long competitionId, Long stageId, List<Long> userIds) {
        scheduleApplicationDecisionEmails(competitionId, stageId, userIds, null, RequestStatus.ACCEPTED);
    }

    private void scheduleRejectedEmailList(
        Long competitionId, Long stageId, List<Long> userIds, String rejectionReason) {
        scheduleApplicationDecisionEmails(competitionId, stageId, userIds, rejectionReason, RequestStatus.REJECTED);
    }

    private List<Long> validateNoDuplicatesOrThrow(List<Long> rawIds) {
        Set<Long> seen = new HashSet<>();
        Set<Long> duplicates = rawIds.stream()
            .filter(id -> !seen.add(id))
            .collect(Collectors.toSet());
        if (!duplicates.isEmpty()) {
            throw new UnableToProcessApplicationException("Duplicate application IDs: " + duplicates);
        }
        return rawIds;
    }

    private boolean isUniqueParticipationConstraint(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            return UNIQUE_PARTICIPATION_CONSTRAINT.equals(cve.getConstraintName());
        }
        return false;
    }

    private void scheduleApplicationDecisionEmails(
        Long competitionId,
        Long stageId,
        List<Long> succeededIds,
        String rejectionReason,
        RequestStatus status) {
        String competitionTitle = getCompetitionInfoOrThrow(competitionId).title();
        String stageTitle = getStageInfoOrThrow(stageId).title();
        List<UserProfileDetails> users = userFacade.findProfilesByIds(succeededIds);

        ApplicationDecisionListEvent event = new ApplicationDecisionListEvent(
            competitionTitle, stageTitle, users, rejectionReason, status);
        emailSender.sendDecisionEmailList(event);
    }

    private void validateApplicationsBelongToTheSameCompetitionAndStage(List<Application> applications) {
        if (applications.isEmpty()) {
            throw new ApplicationNotFoundException("None of the requested applications were found");
        }

        Long expectedCompetitionId = applications.getFirst().getCompetitionId();
        Long expectedStageId = applications.getFirst().getStageId();

        boolean allMatch =
            applications.stream().allMatch(app -> Objects.equals(app.getCompetitionId(), expectedCompetitionId)
                && Objects.equals(app.getStageId(), expectedStageId));

        if (!allMatch) {
            throw new UnableToProcessApplicationException(
                "The applications don't belong to the same competition stage");
        }
    }
}