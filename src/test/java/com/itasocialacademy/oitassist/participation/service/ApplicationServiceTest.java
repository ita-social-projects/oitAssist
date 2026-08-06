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
import com.itasocialacademy.oitassist.participation.dao.dto.event.ApplicationDecisionEvent;
import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateApplicationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import com.itasocialacademy.oitassist.participation.dao.repository.ApplicationRepository;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import com.itasocialacademy.oitassist.participation.exceptions.ApplicationNotFoundException;
import com.itasocialacademy.oitassist.participation.exceptions.UnableToProcessApplicationException;
import com.itasocialacademy.oitassist.participation.exceptions.UserApplicationRequestException;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ApplicationMapper;
import com.itasocialacademy.oitassist.participation.mapper.ParticipationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ProcessApplicationMapper;
import com.itasocialacademy.oitassist.participation.scheduler.AfterCommitScheduler;
import com.itasocialacademy.oitassist.participation.sender.AsyncEmailSender;
import com.itasocialacademy.oitassist.participation.service.ApplicationServiceImpl;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.Instant;
import java.util.Optional;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationMapper applicationMapper;
    @Mock
    private SecurityFacade securityFacade;
    @Mock
    private ParticipationMapper participationMapper;
    @Mock
    private ProcessApplicationMapper processApplicationMapper;
    @Mock
    private CompetitionFacade competitionFacade;
    @Mock
    private AsyncEmailSender emailSender;
    @Mock
    private AfterCommitScheduler scheduler;
    @Mock
    private UserFacade userFacade;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private CreateApplicationRequest createApplicationRequest;
    private Application application;
    private CompetitionDetail competitionDetail;
    private StageDetail stageDetail;

    @BeforeEach
    void setUp() {
        createApplicationRequest = CreateApplicationRequest.builder()
            .competitionId(2L)
            .stageId(3L)
            .build();

        application = new Application();
        application.setId(1L);
        application.setCompetitionId(2L);
        application.setStageId(3L);
        application.setIssuedBy(4L);
        application.setStatus(RequestStatus.PENDING);

        competitionDetail = CompetitionDetail.builder()
            .id(2L)
            .competitionStatus(CompetitionStatus.ENROLLMENT)
            .build();

        stageDetail = StageDetail.builder()
            .id(3L)
            .competitionId(2L)
            .scope(StageScope.DISTRICT)
            .build();

        lenient().when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        lenient().when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        lenient().when(userFacade.findProfileById(4L)).thenReturn(Optional.of(
            new UserProfileDetails(4L, "Test", "test@mail.com")));
    }

    // ---- userApply ----

    @Test
    void userApply_validRequest_shouldSaveAndReturnResponse() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            4L, 2L, 3L, RequestStatus.PENDING)).thenReturn(false);
        when(participationRepository.existsByUserIdAndCompetitionIdAndStageId(4L, 2L, 3L)).thenReturn(false);
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(applicationMapper.toEntity(createApplicationRequest)).thenReturn(application);
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toResponse(application)).thenReturn(getCreateApplicationResponse());

        CreateApplicationResponse response = applicationService.sendEnrollmentRequest(createApplicationRequest);

        assertNotNull(response);
        assertEquals(RequestStatus.PENDING, application.getStatus());
        verify(applicationRepository).save(application);
    }

    @Test
    void userApply_userNotAuthenticated_shouldThrowAuthorizationException() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthorizationException.class,
            () -> applicationService.sendEnrollmentRequest(createApplicationRequest));

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_hasPendingApplication_shouldThrowUserApplicationRequestException() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            4L, 2L, 3L, RequestStatus.PENDING)).thenReturn(true);

        UserApplicationRequestException exception = assertThrows(UserApplicationRequestException.class,
            () -> applicationService.sendEnrollmentRequest(createApplicationRequest));

        assertTrue(exception.getMessage().contains("already has a pending request"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_alreadyParticipant_shouldThrowUserApplicationRequestException() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            4L, 2L, 3L, RequestStatus.PENDING)).thenReturn(false);
        when(participationRepository.existsByUserIdAndCompetitionIdAndStageId(4L, 2L, 3L)).thenReturn(true);

        UserApplicationRequestException exception = assertThrows(UserApplicationRequestException.class,
            () -> applicationService.sendEnrollmentRequest(createApplicationRequest));

        assertTrue(exception.getMessage().contains("already a participant"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_competitionNotFound_shouldThrowCompetitionNotFoundException() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            4L, 2L, 3L, RequestStatus.PENDING)).thenReturn(false);
        when(participationRepository.existsByUserIdAndCompetitionIdAndStageId(4L, 2L, 3L)).thenReturn(false);
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class,
            () -> applicationService.sendEnrollmentRequest(createApplicationRequest));

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_stageNotFound_shouldThrowStageNotFoundException() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            4L, 2L, 3L, RequestStatus.PENDING)).thenReturn(false);
        when(participationRepository.existsByUserIdAndCompetitionIdAndStageId(4L, 2L, 3L)).thenReturn(false);
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class,
            () -> applicationService.sendEnrollmentRequest(createApplicationRequest));

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_competitionNotInEnrollment_shouldThrowUserApplicationRequestException() {
        CompetitionDetail draftCompetition = CompetitionDetail.builder()
            .id(2L)
            .competitionStatus(CompetitionStatus.DRAFT)
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            4L, 2L, 3L, RequestStatus.PENDING)).thenReturn(false);
        when(participationRepository.existsByUserIdAndCompetitionIdAndStageId(4L, 2L, 3L)).thenReturn(false);
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(draftCompetition));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));

        UserApplicationRequestException exception = assertThrows(UserApplicationRequestException.class,
            () -> applicationService.sendEnrollmentRequest(createApplicationRequest));

        assertTrue(exception.getMessage().contains("cannot be enrolled"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_stageWrongScope_shouldThrowUserApplicationRequestException() {
        StageDetail regionalStage = StageDetail.builder()
            .id(3L)
            .competitionId(2L)
            .scope(StageScope.REGIONAL)
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            4L, 2L, 3L, RequestStatus.PENDING)).thenReturn(false);
        when(participationRepository.existsByUserIdAndCompetitionIdAndStageId(4L, 2L, 3L)).thenReturn(false);
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(regionalStage));

        UserApplicationRequestException exception = assertThrows(UserApplicationRequestException.class,
            () -> applicationService.sendEnrollmentRequest(createApplicationRequest));

        assertTrue(exception.getMessage().contains("cannot be enrolled"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_stageDoesNotBelongToCompetition_shouldThrowCompetitionHierarchyValidationException() {
        StageDetail mismatchedStage = StageDetail.builder()
            .id(3L)
            .competitionId(99L)
            .scope(StageScope.DISTRICT)
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            4L, 2L, 3L, RequestStatus.PENDING)).thenReturn(false);
        when(participationRepository.existsByUserIdAndCompetitionIdAndStageId(4L, 2L, 3L)).thenReturn(false);
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(mismatchedStage));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> applicationService.sendEnrollmentRequest(createApplicationRequest));

        assertTrue(exception.getMessage().contains("does not belong to this competition"));
        verify(applicationRepository, never()).save(any());
    }

    // ---- acceptUserApplication ----

    @Test
    void acceptUserApplication_pendingApplication_shouldSaveParticipationAndAccept() {
        Participation participation = Participation.builder()
            .userId(4L)
            .competitionId(2L)
            .stageId(3L)
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(participationMapper.toParticipation(application)).thenReturn(participation);
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);
        when(processApplicationMapper.toResponse(application)).thenReturn(getProcessApplicationResponse(
            RequestStatus.ACCEPTED));

        Instant beforeMethod = Instant.now().minusSeconds(1);
        ProcessApplicationResponse response = applicationService.acceptRequest(1L);

        assertNotNull(response);
        assertEquals(RequestStatus.ACCEPTED, application.getStatus());
        assertEquals(10L, application.getProcessedBy());
        assertNotNull(application.getProcessedAt(), "Processed date should not be null");
        assertTrue(application.getProcessedAt()
            .isAfter(beforeMethod), "Date should be after the start of the test");
        assertTrue(application.getProcessedAt()
            .isBefore(Instant.now().plusSeconds(1)), "Date should not be in the future");
        verify(participationRepository).save(participation);
        verify(applicationRepository).saveAndFlush(application);
    }

    @Test
    void acceptUserApplication_applicationNotFound_shouldThrowApplicationNotFoundException() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class,
            () -> applicationService.acceptRequest(1L));

        verify(participationRepository, never()).save(any());
    }

    @Test
    void acceptUserApplication_applicationNotPending_shouldThrowUnableToProcessApplicationException() {
        application.setStatus(RequestStatus.ACCEPTED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(UnableToProcessApplicationException.class,
            () -> applicationService.acceptRequest(1L));

        verify(participationRepository, never()).save(any());
    }

    // ---- rejectUserApplication ----

    @Test
    void rejectUserApplication_pendingApplication_shouldSetRejectedAndReason() {
        RejectEnrollmentRequest request = new RejectEnrollmentRequest("Invalid profile information");

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);
        when(processApplicationMapper.toResponse(application)).thenReturn(getProcessApplicationResponse(
            RequestStatus.REJECTED));

        Instant beforeMethod = Instant.now().minusSeconds(1);
        ProcessApplicationResponse response = applicationService.rejectRequest(1L, request);

        assertNotNull(response);
        assertEquals(RequestStatus.REJECTED, application.getStatus());
        assertEquals(10L, application.getProcessedBy());
        assertEquals("Invalid profile information", application.getRejectionReason());
        assertNotNull(application.getProcessedAt(), "Processed date should not be null");
        assertTrue(application.getProcessedAt()
            .isAfter(beforeMethod), "Date should be after the start of the test");
        assertTrue(application.getProcessedAt()
            .isBefore(Instant.now().plusSeconds(1)), "Date should not be in the future");
        verify(applicationRepository).saveAndFlush(application);
    }

    @Test
    void rejectUserApplication_applicationNotFound_shouldThrowApplicationNotFoundException() {
        RejectEnrollmentRequest request = new RejectEnrollmentRequest("Invalid profile information");
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class,
            () -> applicationService.rejectRequest(1L, request));

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectUserApplication_applicationNotPending_shouldThrowUnableToProcessApplicationException() {
        RejectEnrollmentRequest request = new RejectEnrollmentRequest("Invalid profile information");
        application.setStatus(RequestStatus.CANCELLED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(UnableToProcessApplicationException.class,
            () -> applicationService.rejectRequest(1L, request));

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    // ---- cancelUserApplication ----

    @Test
    void cancelUserApplication_ownPendingApplication_shouldCancel() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);
        when(processApplicationMapper.toResponse(application)).thenReturn(getProcessApplicationResponse(
            RequestStatus.CANCELLED));

        ProcessApplicationResponse response = applicationService.cancelRequest(1L);

        assertNotNull(response);
        assertEquals(RequestStatus.CANCELLED, application.getStatus());
        verify(applicationRepository).saveAndFlush(application);
    }

    @Test
    void cancelUserApplication_userNotAuthenticated_shouldThrowAuthorizationException() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthorizationException.class,
            () -> applicationService.cancelRequest(1L));

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelUserApplication_applicationNotFound_shouldThrowApplicationNotFoundException() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class,
            () -> applicationService.cancelRequest(1L));

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelUserApplication_applicationNotPending_shouldThrowUnableToProcessApplicationException() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        application.setStatus(RequestStatus.ACCEPTED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(UnableToProcessApplicationException.class,
            () -> applicationService.cancelRequest(1L));

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelUserApplication_notOwnApplication_shouldThrowUnableToProcessApplicationException() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(99L));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        UnableToProcessApplicationException exception = assertThrows(UnableToProcessApplicationException.class,
            () -> applicationService.cancelRequest(1L));

        assertTrue(exception.getMessage().contains("does not belong to the current user"));
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    // ---- helpers ----

    private static CreateApplicationResponse getCreateApplicationResponse() {
        return CreateApplicationResponse.builder()
            .id(1L)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .status(RequestStatus.PENDING)
            .build();
    }

    private static ProcessApplicationResponse getProcessApplicationResponse(RequestStatus status) {
        return ProcessApplicationResponse.builder()
            .id(1L)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .status(status)
            .build();
    }

    // ---- emailSending part ----

    @Test
    void acceptRequest_pendingApplication_shouldScheduleAcceptedEmailAfterCommit() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        applicationService.acceptRequest(1L);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runAfterCommit(runnableCaptor.capture());

        runnableCaptor.getValue().run();

        ArgumentCaptor<ApplicationDecisionEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationDecisionEvent.class);
        verify(emailSender).sendDecisionEmail(eventCaptor.capture());

        ApplicationDecisionEvent event = eventCaptor.getValue();
        assertEquals(RequestStatus.ACCEPTED, event.status());
        assertEquals("test@mail.com", event.email());
        assertNull(event.rejectionReason());
    }

    @Test
    void rejectRequest_pendingApplication_shouldScheduleRejectedEmailAfterCommit() {
        RejectEnrollmentRequest request = new RejectEnrollmentRequest("Incomplete profile");

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(10L));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        applicationService.rejectRequest(1L, request);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runAfterCommit(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        ArgumentCaptor<ApplicationDecisionEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationDecisionEvent.class);
        verify(emailSender).sendDecisionEmail(eventCaptor.capture());

        ApplicationDecisionEvent event = eventCaptor.getValue();
        assertEquals(RequestStatus.REJECTED, event.status());
        assertEquals("Incomplete profile", event.rejectionReason());
        assertEquals("test@mail.com", event.email());
    }
}
