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
import com.itasocialacademy.oitassist.participation.dao.dto.response.FailedInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.SucceededInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.model.Participation;
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
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ProcessInvitationMapper;
import com.itasocialacademy.oitassist.participation.service.interfaces.InvitationService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {
    private static final String ALREADY_PENDING_MESSAGE = "Student already has a pending invitation";
    private static final String PENDING_INVITATION_CONSTRAINT = "idx_unique_pending_invitation";

    private final ParticipationRepository participationRepository;
    private final InvitationRepository invitationRepository;
    private final SecurityFacade securityFacade;
    private final UserFacade userFacade;
    private final InvitationRequestsSaver invitationRequestsSaver;
    private final ParticipationMapper participationMapper;
    private final ProcessInvitationMapper processInvitationMapper;
    private final CompetitionFacade competitionFacade;

    @Override
    public CreateInvitationResponse sendEnrollmentRequest(CreateInvitationRequest request) {
        validateCompetitionAndStageInfo(request.getCompetitionId(), request.getStageId());
        List<Long> studentIds = validateNoDuplicatesOrThrow(request.getStudentIds());

        List<UserAuthDetails> foundUsers = userFacade.findByIds(studentIds);
        Map<Long, UserAuthDetails> foundById = foundUsers.stream()
            .collect(Collectors.toMap(UserAuthDetails::id, u -> u));

        Set<Long> alreadyPending = findStudentsWithPendingInvitations(studentIds, request);
        Set<Long> alreadyParticipants = findParticipants(studentIds, request);

        List<SucceededInvitationResponse> succeeded = new ArrayList<>();
        List<FailedInvitationResponse> failed = new ArrayList<>();

        for (Long studentId : studentIds) {
            UserAuthDetails user = foundById.get(studentId);
            String failureReason = determineFailureReason(studentId, user, alreadyPending, alreadyParticipants);
            if (failureReason != null) {
                failed.add(new FailedInvitationResponse(studentId, failureReason));
                continue;
            }
            try {
                Invitation invitation = invitationRequestsSaver.saveSingleInvitation(studentId, request);
                succeeded.add(new SucceededInvitationResponse(invitation.getId(), studentId));
            } catch (DataIntegrityViolationException e) {
                if (isPendingInvitationConstraintViolation(e)) {
                    failed.add(new FailedInvitationResponse(studentId, ALREADY_PENDING_MESSAGE));
                } else {
                    throw new UnexpectedConstraintViolationException(
                        "Unexpected database constraint violation while saving invitation",
                        ErrorCode.DATA_ACCESS_ERROR,
                        e);
                }
            }
        }
        return CreateInvitationResponse.builder()
            .competitionId(request.getCompetitionId())
            .stageId(request.getStageId())
            .succeeded(succeeded)
            .failed(failed)
            .issuedBy(getCurrentUserIdOrThrow())
            .issuedAt(Instant.now())
            .build();
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
            RequestStatus.PENDING);
        return pendingInvitations.stream().map(Invitation::getStudentId).collect(Collectors.toSet());
    }

    private Set<Long> findParticipants(List<Long> studentIds, CreateInvitationRequest request) {
        List<Participation> participationRecords = participationRepository.findAllByUserIdInAndCompetitionIdAndStageId(
            studentIds,
            request.getCompetitionId(),
            request.getStageId());
        return participationRecords.stream().map(Participation::getUserId).collect(Collectors.toSet());
    }

    private List<Long> validateNoDuplicatesOrThrow(List<Long> rawIds) {
        Set<Long> seen = new HashSet<>();
        Set<Long> duplicates = rawIds.stream()
            .filter(id -> !seen.add(id))
            .collect(Collectors.toSet());
        if (!duplicates.isEmpty()) {
            throw new UserInvitationRequestException("Duplicate student IDs: " + duplicates);
        }
        return rawIds;
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

    private String determineFailureReason(
        Long studentId,
        UserAuthDetails user,
        Set<Long> alreadyPending,
        Set<Long> alreadyParticipants) {
        if (user == null) {
            return "Student not found";
        } else if (user.role() != Role.USER) {
            return "User does not have the required role";
        } else if (alreadyPending.contains(studentId)) {
            return ALREADY_PENDING_MESSAGE;
        } else if (alreadyParticipants.contains(studentId)) {
            return "Student is already a participant";
        }
        return null;
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

    private boolean isPendingInvitationConstraintViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            return PENDING_INVITATION_CONSTRAINT.equals(cve.getConstraintName());
        }
        return false;
    }
}
