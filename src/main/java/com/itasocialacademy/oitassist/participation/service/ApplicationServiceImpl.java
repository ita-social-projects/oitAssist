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
import com.itasocialacademy.oitassist.participation.dao.dto.event.ApplicationDecisionEvent;
import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateApplicationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.EnrollmentRequestsFilter;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.*;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import com.itasocialacademy.oitassist.participation.dao.repository.ApplicationRepository;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import com.itasocialacademy.oitassist.participation.dao.specification.ApplicationSpecification;
import com.itasocialacademy.oitassist.participation.exceptions.ApplicationNotFoundException;
import com.itasocialacademy.oitassist.participation.exceptions.UnableToProcessApplicationException;
import com.itasocialacademy.oitassist.participation.exceptions.UserApplicationRequestException;
import com.itasocialacademy.oitassist.participation.mapper.UserEnrollmentAssembler;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ApplicationMapper;
import com.itasocialacademy.oitassist.participation.mapper.ParticipationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ProcessApplicationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.UserSummaryMapper;
import com.itasocialacademy.oitassist.participation.scheduler.AfterCommitScheduler;
import com.itasocialacademy.oitassist.participation.sender.AsyncEmailSender;
import com.itasocialacademy.oitassist.participation.service.interfaces.ApplicationService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {
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

    @Override
    @Transactional
    public CreateApplicationResponse sendEnrollmentRequest(CreateApplicationRequest createApplicationRequest) {
        Long userId = getCurrentUserIdOrThrow();
        validateUserCanApply(userId, createApplicationRequest);
        Application application = applicationMapper.toEntity(createApplicationRequest);
        application.setStatus(RequestStatus.PENDING);
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
        EnrollmentRequestsFilter request,
        String search,
        Pageable pageable) {
        Long competitionId = request.getCompetitionId();
        Long stageId = request.getStageId();
        validateCompetitionAndStageInfo(competitionId, stageId);
        List<Long> candidateUserIds = applicationRepository.findAll(
            ApplicationSpecification.hasCompetitionAndStage(competitionId, stageId)
                .and(ApplicationSpecification.hasStatus(RequestStatus.PENDING)))
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
                .and(ApplicationSpecification.hasStatus(RequestStatus.PENDING)),
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

    private void validateUserCanApply(Long userId, CreateApplicationRequest request) {
        validateNoPendingApplication(userId, request);
        validateUserDoesNotAlreadyParticipate(userId, request);
        validateCompetitionAndStageInfoForApplying(request.getCompetitionId(), request.getStageId());
    }

    private void validateNoPendingApplication(Long userId, CreateApplicationRequest request) {
        boolean hasPendingApplication = applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            userId,
            request.getCompetitionId(),
            request.getStageId(),
            RequestStatus.PENDING);
        if (hasPendingApplication) {
            throw new UserApplicationRequestException("User already has a pending request");
        }
    }

    private void validateUserDoesNotAlreadyParticipate(Long userId, CreateApplicationRequest request) {
        boolean isParticipant = participationRepository.existsByUserIdAndCompetitionIdAndStageId(
            userId,
            request.getCompetitionId(),
            request.getStageId());
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
        if (application.getStatus() != RequestStatus.PENDING) {
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
}