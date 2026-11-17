package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ApplicationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CompetitionSummary;
import com.itasocialacademy.oitassist.participation.dao.dto.response.InvitationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ParticipationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.StageSummary;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import com.itasocialacademy.oitassist.participation.dao.model.Invitation;
import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import com.itasocialacademy.oitassist.participation.dao.repository.ApplicationRepository;
import com.itasocialacademy.oitassist.participation.dao.repository.InvitationRepository;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import com.itasocialacademy.oitassist.participation.dao.specification.ApplicationSpecification;
import com.itasocialacademy.oitassist.participation.dao.specification.InvitationSpecification;
import com.itasocialacademy.oitassist.participation.dao.specification.ParticipationSpecification;
import com.itasocialacademy.oitassist.participation.service.interfaces.StudentEnrollmentHistoryService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentEnrollmentHistoryServiceImpl implements StudentEnrollmentHistoryService {
    private static final String ORG_ROLE = "ORG";

    private final SecurityFacade securityFacade;
    private final InvitationRepository invitationRepository;
    private final ApplicationRepository applicationRepository;
    private final ParticipationRepository participationRepository;
    private final CompetitionFacade competitionFacade;

    @Override
    @Transactional(readOnly = true)
    public Page<InvitationHistoryItemResponse> getInvitations(Long userId, RequestStatus status, Pageable pageable) {
        checkOwnerOrOrg(userId);
        Page<Invitation> page = invitationRepository.findAll(
            InvitationSpecification.studentIdIn(List.of(userId))
                .and(InvitationSpecification.hasStatus(status)),
            pageable);

        Map<Long, CompetitionSummary> competitions = loadCompetitions(idsOf(page, Invitation::getCompetitionId));
        Map<Long, StageSummary> stages = loadStages(idsOf(page, Invitation::getStageId));

        return page.map(invitation -> new InvitationHistoryItemResponse(
            invitation.getId(),
            invitation.getIssuedAt(),
            invitation.getStatus(),
            competitions.get(invitation.getCompetitionId()),
            stages.get(invitation.getStageId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationHistoryItemResponse> getApplications(Long userId, RequestStatus status, Pageable pageable) {
        checkOwnerOrOrg(userId);
        Page<Application> page = applicationRepository.findAll(
            ApplicationSpecification.userIdIn(List.of(userId))
                .and(ApplicationSpecification.hasStatus(status)),
            pageable);

        Map<Long, CompetitionSummary> competitions =
            loadCompetitions(idsOf(page, Application::getCompetitionId));
        Map<Long, StageSummary> stages =
            loadStages(idsOf(page, Application::getStageId));

        return page.map(application -> new ApplicationHistoryItemResponse(
            application.getId(),
            application.getIssuedAt(),
            application.getStatus(),
            competitions.get(application.getCompetitionId()),
            stages.get(application.getStageId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ParticipationHistoryItemResponse> getParticipations(Long userId, Pageable pageable) {
        checkOwnerOrOrg(userId);
        Page<Participation> page = participationRepository.findAll(
            ParticipationSpecification.userIdIn(List.of(userId)),
            pageable);

        Map<Long, CompetitionSummary> competitions =
            loadCompetitions(idsOf(page, Participation::getCompetitionId));
        Map<Long, StageSummary> stages =
            loadStages(idsOf(page, Participation::getStageId));

        return page.map(participation -> new ParticipationHistoryItemResponse(
            participation.getId(),
            competitions.get(participation.getCompetitionId()),
            stages.get(participation.getStageId())));
    }

    private void checkOwnerOrOrg(Long userId) {
        if (!securityFacade.isOwner(userId) && !securityFacade.hasRole(ORG_ROLE)) {
            throw new InsufficientPermissionsException();
        }
    }

    private Map<Long, CompetitionSummary> loadCompetitions(Set<Long> ids) {
        return competitionFacade.findCompetitionsByIds(ids).stream()
            .collect(Collectors.toMap(
                CompetitionDetail::id,
                detail -> new CompetitionSummary(detail.id(), detail.title())));
    }

    private Map<Long, StageSummary> loadStages(Set<Long> ids) {
        return competitionFacade.findStagesByIds(ids).stream()
            .collect(Collectors.toMap(
                StageDetail::id,
                detail -> new StageSummary(detail.id(), detail.title(), detail.scope())));
    }

    private static <T> Set<Long> idsOf(Page<T> page, Function<T, Long> idExtractor) {
        return page.stream().map(idExtractor).collect(Collectors.toSet());
    }
}
