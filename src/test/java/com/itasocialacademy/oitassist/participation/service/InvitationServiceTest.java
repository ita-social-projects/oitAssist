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
import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import com.itasocialacademy.oitassist.participation.dao.dto.event.InvitationRequestEvent;
import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.InvitationListItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.UserSummary;
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
import com.itasocialacademy.oitassist.participation.mapper.UserEnrollmentAssembler;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ProcessInvitationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.UserSummaryMapper;
import com.itasocialacademy.oitassist.participation.saver.InvitationRequestsSaver;
import com.itasocialacademy.oitassist.participation.sender.AsyncEmailSender;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {
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
    @Mock
    private AsyncEmailSender sender;
    @Mock
    private EmailService emailService;
    @Mock
    private UserSummaryMapper userSummaryMapper;
    @Mock
    private UserEnrollmentAssembler enrollmentAssembler;

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
            .studentIds(List.of(10L)).build();

        invitation = new Invitation();
        invitation.setId(1L);
        invitation.setCompetitionId(2L);
        invitation.setStageId(3L);
        invitation.setStudentId(10L);
        invitation.setIssuedBy(4L);
        invitation.setStatus(RequestStatus.PENDING);

        competitionDetail = CompetitionDetail.builder()
            .id(2L)
            .title("Olympiad")
            .competitionStatus(CompetitionStatus.ENROLLMENT)
            .build();

        stageDetail = StageDetail.builder()
            .id(3L)
            .title("First stage")
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
        when(invitationRequestsSaver.saveSingleInvitation(10L, 2L, 3L)).thenReturn(invitation);

        CreateInvitationResponse response = invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest);

        assertNotNull(response);
        assertEquals(1, response.getSucceeded().size());
        assertEquals(10L, response.getSucceeded().getFirst().studentId());
        assertEquals(1L, response.getSucceeded().getFirst().id());
        assertTrue(response.getFailed().isEmpty());
    }

    @Test
    void sendEnrollmentRequest_duplicateStudentIds_shouldThrowUserInvitationRequestException() {
        CreateInvitationRequest requestWithDuplicates = CreateInvitationRequest.builder()
            .studentIds(List.of(10L, 10L)).build();

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));

        UserInvitationRequestException exception = assertThrows(UserInvitationRequestException.class,
            () -> invitationService.sendInvitationRequests(2L, 3L, requestWithDuplicates));

        assertTrue(exception.getMessage().contains("Duplicate student IDs"));
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any(), any());
    }

    @Test
    void sendEnrollmentRequest_competitionNotFound_shouldThrowCompetitionNotFoundException() {
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class,
            () -> invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest));

        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any(), any());
    }

    @Test
    void sendEnrollmentRequest_stageNotFound_shouldThrowStageNotFoundException() {
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class,
            () -> invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest));

        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any(), any());
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
            () -> invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest));

        assertTrue(exception.getMessage().contains("cannot be enrolled"));
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any(), any());
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
            () -> invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest));

        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any(), any());
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

        CreateInvitationResponse response = invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest);

        assertTrue(response.getSucceeded().isEmpty());
        assertEquals(1, response.getFailed().size());
        assertEquals(10L, response.getFailed().getFirst().studentId());
        assertEquals("Student not found", response.getFailed().getFirst().reason());
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any(), any());
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

        CreateInvitationResponse response = invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest);

        assertTrue(response.getSucceeded().isEmpty());
        assertEquals(1, response.getFailed().size());
        assertEquals("User does not have the required role", response.getFailed().getFirst().reason());
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any(), any());
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

        CreateInvitationResponse response = invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest);

        assertTrue(response.getSucceeded().isEmpty());
        assertEquals(1, response.getFailed().size());
        assertEquals("Student already has a pending invitation", response.getFailed().getFirst().reason());
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any(), any());
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

        CreateInvitationResponse response = invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest);

        assertTrue(response.getSucceeded().isEmpty());
        assertEquals(1, response.getFailed().size());
        verify(invitationRequestsSaver, never()).saveSingleInvitation(any(), any(), any());
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

        when(invitationRequestsSaver.saveSingleInvitation(10L, 2L, 3L))
            .thenThrow(new DataIntegrityViolationException("constraint violation", constraintViolation));

        CreateInvitationResponse response = invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest);

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

        when(invitationRequestsSaver.saveSingleInvitation(10L, 2L, 3L))
            .thenThrow(new DataIntegrityViolationException("constraint violation", unrelatedViolation));

        assertThrows(UnexpectedConstraintViolationException.class,
            () -> invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest));
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

    // ---- emailSending part ----

    @Test
    void sendEnrollmentRequest_withSucceededInvitations_shouldScheduleInvitationEmail() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(invitationRepository.findByStudentIdInAndCompetitionIdAndStageIdAndStatus(
            List.of(10L), 2L, 3L, RequestStatus.PENDING)).thenReturn(List.of());
        when(participationRepository.findAllByUserIdInAndCompetitionIdAndStageId(List.of(10L), 2L, 3L))
            .thenReturn(List.of());
        when(invitationRequestsSaver.saveSingleInvitation(10L, 2L, 3L)).thenReturn(invitation);
        when(userFacade.findByIds(List.of(10L))).thenReturn(List.of(validUser));
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(userFacade.findProfilesByIds(List.of(10L))).thenReturn(
            List.of(new UserProfileDetails(10L, "Test", "Test Surname", "test@mail.com")));
        when(invitationRequestsSaver.saveSingleInvitation(10L, 2L, 3L)).thenReturn(invitation);
        invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest);

        ArgumentCaptor<InvitationRequestEvent> eventCaptor = ArgumentCaptor.forClass(InvitationRequestEvent.class);
        verify(sender).sendInvitationEmail(eventCaptor.capture());

        InvitationRequestEvent event = eventCaptor.getValue();
        assertEquals("Olympiad", event.competitionTitle());
        assertEquals("First stage", event.stageTitle());
        assertEquals(1, event.users().size());
        assertEquals("test@mail.com", event.users().getFirst().email());
    }

    @Test
    void sendEnrollmentRequest_whenAllStudentsFail_shouldNotScheduleInvitationEmail() {
        UserAuthDetails orgUser = new UserAuthDetails(10L, "email1@mail.com", "password1", Role.ORG);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(userFacade.findByIds(List.of(10L))).thenReturn(List.of(orgUser));

        invitationService.sendInvitationRequests(2L, 3L, createInvitationRequest);

        verify(sender, never()).sendInvitationEmail(any());
    }

    @Test
    void sendInvitationEmail_noRecipients_shouldSendNothing() {
        InvitationRequestEvent event = new InvitationRequestEvent("Olympiad", "District Stage", List.of());

        sender.sendInvitationEmail(event);

        verifyNoInteractions(emailService);
    }

    // ---- getInvitations ----

    @Test
    void getEnrollmentRequests_noCandidates_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(invitationRepository.findAll(any(Specification.class))).thenReturn(List.of());
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));

        Page<InvitationListItemResponse> result = invitationService.getEnrollmentRequests(2L, 3L, null, pageable);

        assertTrue(result.isEmpty());
        verify(userFacade, never()).findUserIdsBySearchWithinIds(any(), any());
        verify(invitationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getEnrollmentRequests_noSearch_shouldReturnAllCandidates() {
        Pageable pageable = PageRequest.of(0, 10);

        Invitation candidateInvitation = new Invitation();
        candidateInvitation.setId(1L);
        candidateInvitation.setStudentId(10L);
        candidateInvitation.setIssuedAt(Instant.parse("2026-07-28T10:00:00Z"));
        candidateInvitation.setStatus(RequestStatus.PENDING);

        when(invitationRepository.findAll(any(Specification.class))).thenReturn(List.of(candidateInvitation));
        when(userFacade.findUserIdsBySearchWithinIds(null, List.of(10L))).thenReturn(Optional.empty());
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));

        Page<Invitation> invitationPage = new PageImpl<>(List.of(candidateInvitation), pageable, 1);
        when(invitationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(invitationPage);

        UserProfileDetails user = new UserProfileDetails(10L, "Test", "Test Surname", "test@mail.com");
        when(userSummaryMapper.toUserSummary(user))
            .thenReturn(new UserSummary("Test", "Test Surname", "test@mail.com"));
        when(enrollmentAssembler.enrichWithUser(any(), any(), any())).thenAnswer(invocation -> {
            List<Invitation> apps = invocation.getArgument(0);
            BiFunction<Invitation, UserProfileDetails, InvitationListItemResponse> combiner =
                invocation.getArgument(2);
            return apps.stream()
                .map(app -> combiner.apply(app, user))
                .toList();
        });

        Page<InvitationListItemResponse> result = invitationService.getEnrollmentRequests(2L, 3L, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().getFirst().invitationId());
        assertEquals(RequestStatus.PENDING, result.getContent().getFirst().status());
    }

    @Test
    void getEnrollmentRequests_searchMatchesNoOne_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Invitation candidateInvitation = new Invitation();
        candidateInvitation.setStudentId(10L);

        when(invitationRepository.findAll(any(Specification.class))).thenReturn(List.of(candidateInvitation));
        when(userFacade.findUserIdsBySearchWithinIds("xyz", List.of(10L))).thenReturn(Optional.of(List.of()));
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));

        Page<InvitationListItemResponse> result = invitationService.getEnrollmentRequests(2L, 3L, "xyz", pageable);

        assertTrue(result.isEmpty());
        verify(invitationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getEnrollmentRequests_searchMatchesSubset_shouldFilterByMatchingIds() {
        Pageable pageable = PageRequest.of(0, 10);

        Invitation candidateA = new Invitation();
        candidateA.setStudentId(10L);
        Invitation candidateB = new Invitation();
        candidateB.setStudentId(11L);

        when(invitationRepository.findAll(any(Specification.class))).thenReturn(List.of(candidateA, candidateB));
        when(userFacade.findUserIdsBySearchWithinIds("test", List.of(10L, 11L)))
            .thenReturn(Optional.of(List.of(10L)));
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));

        Invitation matchedInvitation = new Invitation();
        matchedInvitation.setId(5L);
        matchedInvitation.setStudentId(10L);
        matchedInvitation.setIssuedAt(Instant.parse("2026-07-28T10:00:00Z"));
        matchedInvitation.setStatus(RequestStatus.PENDING);

        Page<Invitation> invitationPage = new PageImpl<>(List.of(matchedInvitation), pageable, 1);
        when(invitationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(invitationPage);

        UserProfileDetails user = new UserProfileDetails(10L, "Test", "Test Surname", "test@mail.com");
        when(userSummaryMapper.toUserSummary(user))
            .thenReturn(new UserSummary("Test", "Test Surname", "test@mail.com"));
        when(enrollmentAssembler.enrichWithUser(any(), any(), any())).thenAnswer(invocation -> {
            List<Invitation> apps = invocation.getArgument(0);
            BiFunction<Invitation, UserProfileDetails, InvitationListItemResponse> combiner =
                invocation.getArgument(2);
            return apps.stream()
                .map(app -> combiner.apply(app, user))
                .toList();
        });

        Page<InvitationListItemResponse> result = invitationService.getEnrollmentRequests(2L, 3L, "test", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(5L, result.getContent().getFirst().invitationId());
    }

    @Test
    void getEnrollmentRequests_stageDoesNotBelongToCompetition_shouldThrowCompetitionHierarchyException() {
        Pageable pageable = PageRequest.of(0, 10);

        StageDetail mismatchedStage = StageDetail.builder()
            .id(3L)
            .competitionId(99L)
            .scope(StageScope.DISTRICT)
            .build();

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(mismatchedStage));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> invitationService.getEnrollmentRequests(2L, 3L, null, pageable));

        assertTrue(exception.getMessage().contains("does not belong to this competition"));
        verify(invitationRepository, never()).findAll(any(Specification.class));
    }
}
