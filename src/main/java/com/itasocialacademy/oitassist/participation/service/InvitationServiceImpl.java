package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.participation.saver.InvitationRequestsSaver;
import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Invitation;
import com.itasocialacademy.oitassist.participation.dao.repository.InvitationRepository;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import com.itasocialacademy.oitassist.participation.exceptions.*;
import com.itasocialacademy.oitassist.participation.mapper.ParticipationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.InvitationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ProcessInvitationMapper;
import com.itasocialacademy.oitassist.participation.service.interfaces.InvitationService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {
    private final ParticipationRepository participationRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationMapper invitationMapper;
    private final SecurityFacade securityFacade;
    private final UserFacade userFacade;
    private final InvitationRequestsSaver invitationRequestsSaver;
    private final ParticipationMapper participationMapper;
    private final ProcessInvitationMapper processInvitationMapper;
    private final CompetitionFacade competitionFacade;

    @Override
    public CreateInvitationResponse sendEnrollmentRequest(CreateInvitationRequest request) {
        validateCompetitionAndStageInfo(request.getCompetitionId(), request.getStageId());
        List<Long> studentIds = getAndValidateStudentIdsOrThrow(request);
        List<Long> succeeded = new ArrayList<>();
        List<Long> failed = new ArrayList<>();
        Set<Long> alreadyPending = findStudentsWithPendingInvitations(studentIds, request);
        CreateInvitationResponse createInvitationResponse = CreateInvitationResponse.builder()
            .competitionId(request.getCompetitionId())
            .stageId(request.getStageId())
            .build();
        for (Long studentId : studentIds) {
            if (alreadyPending.contains(studentId)) {
                failed.add(studentId);
                continue;
            }
            try {
                invitationRequestsSaver.saveSingleInvitation(studentId, request);
                succeeded.add(studentId);
            } catch (DataIntegrityViolationException _) {
                failed.add(studentId);
            }
        }
        createInvitationResponse.setSucceeded(succeeded);
        createInvitationResponse.setFailed(failed);
        return createInvitationResponse;
    }

    @Override
    @Transactional
    public ProcessInvitationResponse acceptRequest(Long invitationId) {
        Invitation invitation = getPendingInvitationOrThrow(invitationId);
        Long userId = getCurrentUserIdOrThrow();
        validateUserCanProcessInvitation(userId, invitation);
        invitation.setStatus(RequestStatus.ACCEPTED);
        invitation.setProcessedAt(Instant.now());
        participationRepository.save(participationMapper.toParticipation(invitation));
        return processInvitationMapper.toResponse(invitationRepository.saveAndFlush(invitation));
    }

    @Override
    @Transactional
    public ProcessInvitationResponse rejectRequest(Long invitationId, RejectEnrollmentRequest request) {
        Invitation invitation = getPendingInvitationOrThrow(invitationId);
        Long userId = getCurrentUserIdOrThrow();
        validateUserCanProcessInvitation(userId, invitation);
        invitation.setStatus(RequestStatus.REJECTED);
        invitation.setProcessedAt(Instant.now());
        invitation.setRejectionReason(request.rejectionReason());
        return processInvitationMapper.toResponse(invitationRepository.saveAndFlush(invitation));
    }

    @Override
    @Transactional
    public ProcessInvitationResponse cancelRequest(Long invitationId) {
        Invitation invitation = getPendingInvitationOrThrow(invitationId);
        Long userId = getCurrentUserIdOrThrow();
        validateUserCanCancelInvitation(userId, invitation);
        invitation.setStatus(RequestStatus.CANCELLED);
        invitation.setProcessedAt(Instant.now());
        return processInvitationMapper.toResponse(invitationRepository.saveAndFlush(invitation));
    }

    private Set<Long> findStudentsWithPendingInvitations(List<Long> studentIds, CreateInvitationRequest request) {
        List<Invitation> pendingInvitations = invitationRepository.findByStudentIdInAndCompetitionIdAndStageIdAndStatus(
            studentIds,
            request.getCompetitionId(),
            request.getStageId(),
            RequestStatus.PENDING
        );
        return pendingInvitations.stream().map(Invitation::getStudentId).collect(Collectors.toSet());
    }

    private List<Long> getAndValidateStudentIdsOrThrow(CreateInvitationRequest request) {
        List<Long> rawIds = request.getStudentIds();
        Set<Long> seen = new HashSet<>();
        Set<Long> duplicates = rawIds.stream()
            .filter(id -> !seen.add(id))
            .collect(Collectors.toSet());
        if (!duplicates.isEmpty()) {
            throw new UserInvitationRequestException("Duplicate student IDs: " + duplicates);
        }

        List<UserAuthDetails> foundUsers = userFacade.findByIds(rawIds);
        Set<Long> foundIds = foundUsers.stream()
            .map(UserAuthDetails::id)
            .collect(Collectors.toSet());
        List<Long> missingIds = rawIds.stream()
            .filter(id -> !foundIds.contains(id)).toList();

        if (!missingIds.isEmpty()) {
            throw new UserInvitationRequestException("Students with IDs: " + missingIds + " not found");
        }
        List<Long> wrongRoleIds = foundUsers.stream()
            .filter(user -> user.role() != Role.USER)
            .map(UserAuthDetails::id)
            .toList();
        if (!wrongRoleIds.isEmpty()) {
            throw new UserInvitationRequestException("Users do not have the required role: " + wrongRoleIds);
        }
        return new ArrayList<>(rawIds);
    }

    private void validateCompetitionAndStageInfo(Long competitionId, Long stageId) {
        CompetitionDetail competitionDetail = competitionFacade.findCompetitionById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));
        StageDetail stageDetail = competitionFacade.findStageById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));
        if (competitionDetail.competitionStatus() != CompetitionStatus.ENROLLMENT) {
            throw new UserInvitationRequestException("The competition cannot be enrolled");
        }
        if (!stageDetail.competitionId().equals(competitionId)) {
            throw new CompetitionHierarchyValidationException("Specified stage does not belong to this competition");
        }
    }

    private Invitation getPendingInvitationOrThrow(Long applicationId) {
        Invitation invitation = invitationRepository.findById(applicationId)
            .orElseThrow(() -> new InvitationNotFoundException("The invitation was not found"));
        if (invitation.getStatus() != RequestStatus.PENDING) {
            throw new UnableToProcessInvitationException("The invitation request is not in the PENDING status");
        }
        return invitation;
    }

    private Long getCurrentUserIdOrThrow() {
        return securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User is not authenticated",
                ErrorCode.ACCESS_DENIED));
    }

    private void validateUserCanProcessInvitation(Long userId, Invitation invitation) {
        if (!userId.equals(invitation.getUserId())) {
            throw new UnableToProcessInvitationException("The invitation is not issued to the current user");
        }
    }

    private void validateUserCanCancelInvitation(Long userId, Invitation invitation) {
        if (!userId.equals(invitation.getIssuedBy())) {
            throw new UnableToProcessInvitationException("The invitation is not issued by the current user");
        }
    }
}
