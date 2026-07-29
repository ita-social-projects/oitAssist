package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Invitation;
import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import com.itasocialacademy.oitassist.participation.dao.repository.InvitationRepository;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import com.itasocialacademy.oitassist.participation.exceptions.InvitationNotFoundException;
import com.itasocialacademy.oitassist.participation.exceptions.UnableToProcessInvitationException;
import com.itasocialacademy.oitassist.participation.exceptions.UnexpectedConstraintViolationException;
import com.itasocialacademy.oitassist.participation.exceptions.UserInvitationRequestException;
import com.itasocialacademy.oitassist.participation.mapper.ParticipationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ProcessInvitationMapper;
import com.itasocialacademy.oitassist.participation.saver.InvitationRequestsSaver;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvitationServiceTest {
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private SecurityFacade securityFacade;
    @Mock
    private UserFacade userFacade;
    @Mock
    private InvitationRequestsSaver invitationRequestsSaver;
    @Mock
    private ParticipationMapper participationMapper;
    @Mock
    private ProcessInvitationMapper processInvitationMapper;
    @Mock
    private CompetitionFacade competitionFacade;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    private CreateInvitationRequest createInvitationRequest;
    private Invitation invitation;
    private CompetitionDetail competitionDetail;
    private StageDetail stageDetail;
    private UserAuthDetails validUser;

    @BeforeEach
    void setUp() {
        createInvitationRequest = CreateInvitationRequest.builder()
            .competitionId(2L)
            .stageId(3L)
            .studentIds(List.of(10L))
            .build();

        invitation = new Invitation();
        invitation.setId(1L);
        invitation.setCompetitionId(2L);
        invitation.setStageId(3L);
        invitation.setStudentId(10L);
        invitation.setIssuedBy(4L);
        invitation.setStatus(RequestStatus.PENDING);

        competitionDetail = CompetitionDetail.builder()
            .id(2L)
            .competitionStatus(CompetitionStatus.ENROLLMENT)
            .build();

        stageDetail = StageDetail.builder()
            .id(3L)
            .competitionId(2L)
            .scope(StageScope.DISTRICT)
            .build();

        validUser = new UserAuthDetails(10L, "email1@mail.com", "password1", Role.USER);
    }

    // ---- sendInvitation ----

    @Test
    void sendEnrollmentRequest_validRequest_shouldSaveAndReturnSucceeded() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(userFacade.findByIds(List.of(10L))).thenReturn(List.of(validUser));
        when(invitationRepository.findByStudentIdInAndCompetitionIdAndStageIdAndStatus(
            List.of(10L), 2L, 3L, RequestStatus.PENDING)).thenReturn(List.of());
        when(participationRepository.findAllByUserIdInAndCompetitionIdAndStageId(List.of(10L), 2L, 3L))
            .thenReturn(List.of());
        when(invitationRequestsSaver.saveSingleInvitation(10L, createInvitationRequest)).thenReturn(invitation);

        CreateInvitationResponse response = invitationService.sendEnrollmentRequest(createInvitationRequest);

        assertNotNull(response);
        assertEquals(1, response.getSucceeded().size());
        assertEquals(10L, response.getSucceeded().getFirst().studentId());
        assertEquals(1L, response.getSucceeded().getFirst().id());
        assertTrue(response.getFailed().isEmpty());
    }

    @Test
    void sendEnrollmentRequest_duplicateStudentIds_shouldThrowUserInvitationRequestException() {
        CreateInvitationRequest requestWithDuplicates = CreateInvitationRequest.builder()
            .competitionId(2L)
            .stageId(3L)
            .studentIds(List.of(10L, 10L))
            .build();

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));

        UserInvitationRequestException exception = assertThrows(UserInvitationRequestException.class,
            () -> invitationService.sendEnrollmentRequest(requestWithDuplicates));

        assertTrue(exception.getMessage().contains("Duplicate student IDs"));
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any());
    }

    @Test
    void sendEnrollmentRequest_competitionNotFound_shouldThrowCompetitionNotFoundException() {
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class,
            () -> invitationService.sendEnrollmentRequest(createInvitationRequest));

        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any());
    }

    @Test
    void sendEnrollmentRequest_stageNotFound_shouldThrowStageNotFoundException() {
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class,
            () -> invitationService.sendEnrollmentRequest(createInvitationRequest));

        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any());
    }

    @Test
    void sendEnrollmentRequest_competitionNotInEnrollment_shouldThrowUserInvitationRequestException() {
        CompetitionDetail draftCompetition = CompetitionDetail.builder()
            .id(2L)
            .competitionStatus(CompetitionStatus.DRAFT)
            .build();

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(draftCompetition));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));

        UserInvitationRequestException exception = assertThrows(UserInvitationRequestException.class,
            () -> invitationService.sendEnrollmentRequest(createInvitationRequest));

        assertTrue(exception.getMessage().contains("cannot be enrolled"));
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any());
    }

    @Test
    void sendEnrollmentRequest_stageDoesNotBelongToCompetition_shouldThrowCompetitionHierarchyValidationException() {
        StageDetail mismatchedStage = StageDetail.builder()
            .id(3L)
            .competitionId(99L)
            .scope(StageScope.DISTRICT)
            .build();

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(mismatchedStage));

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> invitationService.sendEnrollmentRequest(createInvitationRequest));

        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any());
    }

    @Test
    void sendEnrollmentRequest_studentNotFound_shouldAddToFailed() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(userFacade.findByIds(List.of(10L))).thenReturn(List.of());
        when(invitationRepository.findByStudentIdInAndCompetitionIdAndStageIdAndStatus(
            List.of(10L), 2L, 3L, RequestStatus.PENDING)).thenReturn(List.of());
        when(participationRepository.findAllByUserIdInAndCompetitionIdAndStageId(List.of(10L), 2L, 3L))
            .thenReturn(List.of());

        CreateInvitationResponse response = invitationService.sendEnrollmentRequest(createInvitationRequest);

        assertTrue(response.getSucceeded().isEmpty());
        assertEquals(1, response.getFailed().size());
        assertEquals(10L, response.getFailed().getFirst().studentId());
        assertEquals("Student not found", response.getFailed().getFirst().reason());
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any());
    }

    @Test
    void sendEnrollmentRequest_wrongRole_shouldAddToFailed() {
        UserAuthDetails orgUser = new UserAuthDetails(10L, "email1@mail.com", "password1", Role.ORG);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(userFacade.findByIds(List.of(10L))).thenReturn(List.of(orgUser));
        when(invitationRepository.findByStudentIdInAndCompetitionIdAndStageIdAndStatus(
            List.of(10L), 2L, 3L, RequestStatus.PENDING)).thenReturn(List.of());
        when(participationRepository.findAllByUserIdInAndCompetitionIdAndStageId(List.of(10L), 2L, 3L))
            .thenReturn(List.of());

        CreateInvitationResponse response = invitationService.sendEnrollmentRequest(createInvitationRequest);

        assertTrue(response.getSucceeded().isEmpty());
        assertEquals(1, response.getFailed().size());
        assertEquals("User does not have the required role", response.getFailed().getFirst().reason());
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any());
    }

    @Test
    void sendEnrollmentRequest_alreadyPendingInvitation_shouldAddToFailed() {
        Invitation existingPending = new Invitation();
        existingPending.setStudentId(10L);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(userFacade.findByIds(List.of(10L))).thenReturn(List.of(validUser));
        when(invitationRepository.findByStudentIdInAndCompetitionIdAndStageIdAndStatus(
            List.of(10L), 2L, 3L, RequestStatus.PENDING)).thenReturn(List.of(existingPending));
        when(participationRepository.findAllByUserIdInAndCompetitionIdAndStageId(List.of(10L), 2L, 3L))
            .thenReturn(List.of());

        CreateInvitationResponse response = invitationService.sendEnrollmentRequest(createInvitationRequest);

        assertTrue(response.getSucceeded().isEmpty());
        assertEquals(1, response.getFailed().size());
        assertEquals("Student already has a pending invitation", response.getFailed().getFirst().reason());
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any());
    }

    @Test
    void sendEnrollmentRequest_alreadyParticipant_shouldAddToFailed() {
        Participation participation = Participation.builder()
            .userId(10L)
            .competitionId(2L)
            .stageId(3L)
            .build();
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(userFacade.findByIds(List.of(10L))).thenReturn(List.of(validUser));
        when(invitationRepository.findByStudentIdInAndCompetitionIdAndStageIdAndStatus(
            List.of(10L), 2L, 3L, RequestStatus.PENDING)).thenReturn(List.of());
        when(participationRepository.findAllByUserIdInAndCompetitionIdAndStageId(List.of(10L), 2L, 3L))
            .thenReturn(List.of(participation));

        CreateInvitationResponse response = invitationService.sendEnrollmentRequest(createInvitationRequest);

        assertTrue(response.getSucceeded().isEmpty());
        assertEquals(1, response.getFailed().size());
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any());
    }

    @Test
    void sendEnrollmentRequest_raceConditionOnSave_shouldAddToFailed() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(userFacade.findByIds(List.of(10L))).thenReturn(List.of(validUser));
        when(invitationRepository.findByStudentIdInAndCompetitionIdAndStageIdAndStatus(
            List.of(10L), 2L, 3L, RequestStatus.PENDING)).thenReturn(List.of());
        when(participationRepository.findAllByUserIdInAndCompetitionIdAndStageId(List.of(10L), 2L, 3L))
            .thenReturn(List.of());
        ConstraintViolationException constraintViolation = new ConstraintViolationException(
            "duplicate pending invitation", null, "idx_unique_pending_invitation");

        when(invitationRequestsSaver.saveSingleInvitation(10L, createInvitationRequest))
            .thenThrow(new DataIntegrityViolationException("constraint violation", constraintViolation));

        CreateInvitationResponse response = invitationService.sendEnrollmentRequest(createInvitationRequest);

        assertTrue(response.getSucceeded().isEmpty());
        assertEquals(1, response.getFailed().size());
        assertEquals("Student already has a pending invitation", response.getFailed().getFirst().reason());
    }

    @Test
    void sendEnrollmentRequest_unrelatedConstraintViolation_shouldThrowUnexpectedConstraintViolationException() {
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(userFacade.findByIds(List.of(10L))).thenReturn(List.of(validUser));
        when(invitationRepository.findByStudentIdInAndCompetitionIdAndStageIdAndStatus(
            List.of(10L), 2L, 3L, RequestStatus.PENDING)).thenReturn(List.of());
        when(participationRepository.findAllByUserIdInAndCompetitionIdAndStageId(List.of(10L), 2L, 3L))
            .thenReturn(List.of());

        ConstraintViolationException unrelatedViolation = new ConstraintViolationException(
            "not-null violation", null, "some_other_constraint");

        when(invitationRequestsSaver.saveSingleInvitation(10L, createInvitationRequest))
            .thenThrow(new DataIntegrityViolationException("constraint violation", unrelatedViolation));

        assertThrows(UnexpectedConstraintViolationException.class,
            () -> invitationService.sendEnrollmentRequest(createInvitationRequest));
    }

    // ---- acceptRequest ----

    @Test
    void acceptRequest_pendingInvitation_shouldSaveParticipationAndAccept() {
        Participation participation = Participation.builder()
            .userId(10L)
            .competitionId(2L)
            .stageId(3L)
            .build();

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(participationMapper.toParticipation(invitation)).thenReturn(participation);
        when(invitationRepository.saveAndFlush(invitation)).thenReturn(invitation);
        when(processInvitationMapper.toResponse(invitation)).thenReturn(getProcessInvitationResponse(
            RequestStatus.ACCEPTED));

        ProcessInvitationResponse response = invitationService.acceptRequest(1L);

        assertNotNull(response);
        assertEquals(RequestStatus.ACCEPTED, invitation.getStatus());
        assertNotNull(invitation.getProcessedAt());
        verify(participationRepository).save(participation);
        verify(invitationRepository).saveAndFlush(invitation);
    }

    @Test
    void acceptRequest_invitationNotFound_shouldThrowInvitationNotFoundException() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(InvitationNotFoundException.class, () -> invitationService.acceptRequest(1L));

        verify(participationRepository, never()).save(any());
    }

    @Test
    void acceptRequest_invitationNotPending_shouldThrowUnableToProcessInvitationException() {
        invitation.setStatus(RequestStatus.ACCEPTED);
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        assertThrows(UnableToProcessInvitationException.class, () -> invitationService.acceptRequest(1L));

        verify(participationRepository, never()).save(any());
    }

    @Test
    void acceptRequest_notOwnInvitation_shouldThrowUnableToProcessInvitationException() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(99L));

        UnableToProcessInvitationException exception = assertThrows(UnableToProcessInvitationException.class,
            () -> invitationService.acceptRequest(1L));

        assertTrue(exception.getMessage().contains("not issued to the current user"));
        verify(participationRepository, never()).save(any());
    }

    // ---- rejectRequest ----

    @Test
    void rejectRequest_pendingInvitation_shouldSetRejectedAndReason() {
        RejectEnrollmentRequest request = new RejectEnrollmentRequest("Not interested");

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(invitationRepository.saveAndFlush(invitation)).thenReturn(invitation);
        when(processInvitationMapper.toResponse(invitation)).thenReturn(getProcessInvitationResponse(
            RequestStatus.REJECTED));

        ProcessInvitationResponse response = invitationService.rejectRequest(1L, request);

        assertNotNull(response);
        assertEquals(RequestStatus.REJECTED, invitation.getStatus());
        assertEquals("Not interested", invitation.getRejectionReason());
        assertNotNull(invitation.getProcessedAt());
        verify(invitationRepository).saveAndFlush(invitation);
    }

    @Test
    void rejectRequest_invitationNotFound_shouldThrowInvitationNotFoundException() {
        RejectEnrollmentRequest request = new RejectEnrollmentRequest("Not interested");
        when(invitationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(InvitationNotFoundException.class, () -> invitationService.rejectRequest(1L, request));

        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectRequest_invitationNotPending_shouldThrowUnableToProcessInvitationException() {
        RejectEnrollmentRequest request = new RejectEnrollmentRequest("Not interested");
        invitation.setStatus(RequestStatus.CANCELLED);
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        assertThrows(UnableToProcessInvitationException.class,
            () -> invitationService.rejectRequest(1L, request));

        verify(invitationRepository, never()).saveAndFlush(any());
    }

    // ---- cancelRequest ----

    @Test
    void cancelRequest_ownPendingInvitation_shouldCancel() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(invitationRepository.saveAndFlush(invitation)).thenReturn(invitation);
        when(processInvitationMapper.toResponse(invitation)).thenReturn(getProcessInvitationResponse(
            RequestStatus.CANCELLED));

        ProcessInvitationResponse response = invitationService.cancelRequest(1L);

        assertNotNull(response);
        assertEquals(RequestStatus.CANCELLED, invitation.getStatus());
        verify(invitationRepository).saveAndFlush(invitation);
    }

    @Test
    void cancelRequest_userNotAuthenticated_shouldThrowAuthorizationException() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthorizationException.class, () -> invitationService.cancelRequest(1L));

        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelRequest_invitationNotFound_shouldThrowInvitationNotFoundException() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(InvitationNotFoundException.class, () -> invitationService.cancelRequest(1L));

        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelRequest_invitationNotPending_shouldThrowUnableToProcessInvitationException() {
        invitation.setStatus(RequestStatus.ACCEPTED);
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        assertThrows(UnableToProcessInvitationException.class, () -> invitationService.cancelRequest(1L));

        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelRequest_notOwnInvitation_shouldThrowUnableToProcessInvitationException() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(99L));

        UnableToProcessInvitationException exception = assertThrows(UnableToProcessInvitationException.class,
            () -> invitationService.cancelRequest(1L));

        assertTrue(exception.getMessage().contains("not issued by the current user"));
        verify(invitationRepository, never()).saveAndFlush(any());
    }

    // ---- helpers ----

    private static ProcessInvitationResponse getProcessInvitationResponse(RequestStatus status) {
        return ProcessInvitationResponse.builder()
            .id(1L)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .studentId(10L)
            .status(status)
            .build();
    }
}
