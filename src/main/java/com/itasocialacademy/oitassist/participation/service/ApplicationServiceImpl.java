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
import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import com.itasocialacademy.oitassist.participation.dao.dto.event.ApplicationAcceptedEvent;
import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateApplicationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import com.itasocialacademy.oitassist.participation.dao.repository.ApplicationRepository;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import com.itasocialacademy.oitassist.participation.exceptions.ApplicationNotFoundException;
import com.itasocialacademy.oitassist.participation.exceptions.UnableToProcessApplicationException;
import com.itasocialacademy.oitassist.participation.exceptions.UserApplicationRequestException;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ApplicationMapper;
import com.itasocialacademy.oitassist.participation.mapper.ParticipationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ProcessApplicationMapper;
import com.itasocialacademy.oitassist.participation.sender.AsyncEmailSender;
import com.itasocialacademy.oitassist.participation.service.interfaces.ApplicationService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

        scheduleDecisionEmailAfterCommit(
            application.getCompetitionId(),
            application.getStageId(),
            application.getIssuedBy()
        );

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
        return processApplicationMapper.toResponse(applicationRepository.saveAndFlush(application));
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

    private void validateUserCanApply(Long userId, CreateApplicationRequest createApplicationRequest) {
        validateNoPendingApplication(userId, createApplicationRequest);
        validateUserDoesNotAlreadyParticipate(userId, createApplicationRequest);
        validateCompetitionAndStageInfo(createApplicationRequest);
    }

    private void validateNoPendingApplication(Long userId, CreateApplicationRequest createApplicationRequest) {
        boolean hasPendingApplication = applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            userId,
            createApplicationRequest.getCompetitionId(),
            createApplicationRequest.getStageId(),
            RequestStatus.PENDING);
        if (hasPendingApplication) {
            throw new UserApplicationRequestException("User already has a pending request");
        }
    }

    private void validateUserDoesNotAlreadyParticipate(Long userId, CreateApplicationRequest createApplicationRequest) {
        boolean isParticipant = participationRepository.existsByUserIdAndCompetitionIdAndStageId(
            userId,
            createApplicationRequest.getCompetitionId(),
            createApplicationRequest.getStageId());
        if (isParticipant) {
            throw new UserApplicationRequestException("User is already a participant");
        }
    }

    private void validateCompetitionAndStageInfo(CreateApplicationRequest createApplicationRequest) {
        Long competitionId = createApplicationRequest.getCompetitionId();
        Long stageId = createApplicationRequest.getStageId();
        CompetitionDetail competitionDetail = getCompetitionInfoOrThrow(competitionId);
        StageDetail stageDetail = getStageInfoOrThrow(stageId);
        if (competitionDetail.competitionStatus() != CompetitionStatus.ENROLLMENT) {
            throw new UserApplicationRequestException("The competition cannot be enrolled");
        }
        if (stageDetail.scope() != StageScope.DISTRICT && stageDetail.scope() != StageScope.CITY) {
            throw new UserApplicationRequestException("The specified stage cannot be enrolled");
        }
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

    private void scheduleDecisionEmailAfterCommit(Long competitionId, Long stageId, Long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        String competitionTitle = getCompetitionInfoOrThrow(competitionId).title();
        String stageTitle = getStageInfoOrThrow(stageId).title();
        UserA
        String email = userFacade.findByIds(List.of(userId)).getFirst().email();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailSender.sendDecisionEmail(new ApplicationAcceptedEvent(competitionTitle, stageTitle, email));
            }
        });
    }

    private CompetitionDetail getCompetitionInfoOrThrow(Long competitionId) {
        return competitionFacade.findCompetitionById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));
    }

    private StageDetail getStageInfoOrThrow(Long stageId) {
        return competitionFacade.findStageById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));
    }
}