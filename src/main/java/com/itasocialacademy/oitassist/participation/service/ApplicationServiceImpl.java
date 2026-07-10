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
import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateApplicationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectApplicationRequest;
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
import com.itasocialacademy.oitassist.participation.service.interfaces.ApplicationService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
    @Transactional
    public CreateApplicationResponse userApply(CreateApplicationRequest createApplicationRequest) {
        Long userId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User is not authenticated",
                ErrorCode.ACCESS_DENIED));
        validateUserCanApply(userId, createApplicationRequest);
        Application application = applicationMapper.toEntity(createApplicationRequest);
        application.setStatus(RequestStatus.PENDING);
        return applicationMapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional
    public ProcessApplicationResponse acceptUserApplication(Long applicationId) {
        Application application = getPendingApplicationOrThrow(applicationId);
        participationRepository.save(participationMapper.toParticipation(application));
        application.setStatus(RequestStatus.ACCEPTED);
        return processApplicationMapper.toResponse(applicationRepository.saveAndFlush(application));
    }

    @Override
    @Transactional
    public ProcessApplicationResponse rejectUserApplication(Long applicationId, RejectApplicationRequest request) {
        Application application = getPendingApplicationOrThrow(applicationId);
        application.setStatus(RequestStatus.REJECTED);
        application.setRejectionReason(request.rejectionReason());
        return processApplicationMapper.toResponse(applicationRepository.saveAndFlush(application));
    }

    @Override
    @Transactional
    public ProcessApplicationResponse cancelUserApplication(Long applicationId) {
        Long userId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User is not authenticated",
                ErrorCode.ACCESS_DENIED));
        Application application = getPendingApplicationOrThrow(applicationId);
        validateUserCanCancelApplication(userId, application);
        application.setStatus(RequestStatus.CANCELLED);
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
            createApplicationRequest.competitionId(),
            createApplicationRequest.stageId(),
            RequestStatus.PENDING);
        if (hasPendingApplication) {
            throw new UserApplicationRequestException("User already has a pending request");
        }
    }

    private void validateUserDoesNotAlreadyParticipate(Long userId, CreateApplicationRequest createApplicationRequest) {
        boolean isParticipant = participationRepository.existsByUserIdAndCompetitionIdAndStageId(
            userId,
            createApplicationRequest.competitionId(),
            createApplicationRequest.stageId());
        if (isParticipant) {
            throw new UserApplicationRequestException("User is already a participant");
        }
    }

    private void validateCompetitionAndStageInfo(CreateApplicationRequest createApplicationRequest) {
        Long competitionId = createApplicationRequest.competitionId();
        Long stageId = createApplicationRequest.stageId();
        CompetitionDetail competitionDetail = competitionFacade.findCompetitionById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));
        StageDetail stageDetail = competitionFacade.findStageById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));
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
        if (!application.getIssuedBy().equals(userId)) {
            throw new UnableToProcessApplicationException("The application does not belong to the current user");
        }
    }
}