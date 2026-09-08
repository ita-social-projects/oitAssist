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
import com.itasocialacademy.oitassist.participation.components.saver.ApplicationDecisionsSaver;
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
import com.itasocialacademy.oitassist.participation.exceptions.ApplicationNotFoundException;
import com.itasocialacademy.oitassist.participation.exceptions.UnableToProcessApplicationException;
import com.itasocialacademy.oitassist.participation.exceptions.UserApplicationRequestException;
import com.itasocialacademy.oitassist.participation.mapper.UserEnrollmentAssembler;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ApplicationMapper;
import com.itasocialacademy.oitassist.participation.mapper.ParticipationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.ProcessApplicationMapper;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.UserSummaryMapper;
import com.itasocialacademy.oitassist.participation.components.scheduler.AfterCommitScheduler;
import com.itasocialacademy.oitassist.participation.components.sender.AsyncEmailSender;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
    private static final String PROFILE_REJECTION_REASON = "Invalid profile information";

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
    @Mock
    private UserSummaryMapper userSummaryMapper;
    @Mock
    private UserEnrollmentAssembler enrollmentAssembler;
    @Captor
    private ArgumentCaptor<Application> applicationCaptor;
    @Mock
    private ApplicationDecisionsSaver applicationSaver;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private Application application;

    @BeforeEach
    void setUp() {
        application = new Application();
        application.setId(1L);
        application.setCompetitionId(2L);
        application.setStageId(3L);
        application.setIssuedBy(4L);
        application.setStatus(RequestStatus.PENDING);

        CompetitionDetail competitionDetail = CompetitionDetail.builder()
            .id(2L)
            .competitionStatus(CompetitionStatus.ENROLLMENT)
            .build();

        StageDetail stageDetail = StageDetail.builder()
            .id(3L)
            .competitionId(2L)
            .scope(StageScope.DISTRICT)
            .build();

        lenient().when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        lenient().when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        lenient().when(userFacade.findProfileById(4L)).thenReturn(Optional.of(
            new UserProfileDetails(4L, "Test", "Test Surname", "test@mail.com")));
        lenient().when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(4L));
        lenient().when(applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            4L, 2L, 3L, RequestStatus.PENDING)).thenReturn(false);
        lenient().when(participationRepository.existsByUserIdAndCompetitionIdAndStageId(4L, 2L, 3L)).thenReturn(false);
    }

    // ---- userApply ----

    @Test
    void userApply_validRequest_shouldSaveAndReturnResponse() {

        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(getCreateApplicationResponse());

        CreateApplicationResponse response = applicationService.sendApplicationRequest(2L, 3L);

        verify(applicationRepository).save(applicationCaptor.capture());
        Application savedApplication = applicationCaptor.getValue();

        assertEquals(RequestStatus.PENDING, savedApplication.getStatus());
        assertEquals(2L, savedApplication.getCompetitionId());
        assertEquals(3L, savedApplication.getStageId());
        assertNotNull(response);
    }

    @Test
    void userApply_userNotAuthenticated_shouldThrowAuthorizationException() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthorizationException.class,
            () -> applicationService.sendApplicationRequest(2L, 3L));

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_hasPendingApplication_shouldThrowUserApplicationRequestException() {
        when(applicationRepository.existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
            4L, 2L, 3L, RequestStatus.PENDING)).thenReturn(true);

        UserApplicationRequestException exception = assertThrows(UserApplicationRequestException.class,
            () -> applicationService.sendApplicationRequest(2L, 3L));

        assertTrue(exception.getMessage().contains("already has a pending request"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_alreadyParticipant_shouldThrowUserApplicationRequestException() {
        when(participationRepository.existsByUserIdAndCompetitionIdAndStageId(4L, 2L, 3L)).thenReturn(true);

        UserApplicationRequestException exception = assertThrows(UserApplicationRequestException.class,
            () -> applicationService.sendApplicationRequest(2L, 3L));

        assertTrue(exception.getMessage().contains("already a participant"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_competitionNotFound_shouldThrowCompetitionNotFoundException() {
        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class,
            () -> applicationService.sendApplicationRequest(2L, 3L));

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_stageNotFound_shouldThrowStageNotFoundException() {
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class,
            () -> applicationService.sendApplicationRequest(2L, 3L));

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void userApply_competitionNotInEnrollment_shouldThrowUserApplicationRequestException() {
        CompetitionDetail draftCompetition = CompetitionDetail.builder()
            .id(2L)
            .competitionStatus(CompetitionStatus.DRAFT)
            .build();

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(draftCompetition));

        UserApplicationRequestException exception = assertThrows(UserApplicationRequestException.class,
            () -> applicationService.sendApplicationRequest(2L, 3L));

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

        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(regionalStage));

        UserApplicationRequestException exception = assertThrows(UserApplicationRequestException.class,
            () -> applicationService.sendApplicationRequest(2L, 3L));

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

        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(mismatchedStage));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> applicationService.sendApplicationRequest(2L, 3L));

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

    // ---- acceptApplicationList ----

    @Test
    void acceptApplicationList_validRequest_shouldAcceptAllAndScheduleEmail() {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L, 2L));

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);
        app1.setIssuedBy(10L);
        app1.setStatus(RequestStatus.PENDING);

        Application app2 = new Application();
        app2.setId(2L);
        app2.setCompetitionId(2L);
        app2.setStageId(3L);
        app2.setIssuedBy(11L);
        app2.setStatus(RequestStatus.PENDING);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1, app2));

        Participation participation1 = Participation.builder().userId(10L).competitionId(2L).stageId(3L).build();
        Participation participation2 = Participation.builder().userId(11L).competitionId(2L).stageId(3L).build();

        when(applicationSaver.saveAcceptedApplicationData(4L, app1, 2L, 3L)).thenReturn(participation1);
        when(applicationSaver.saveAcceptedApplicationData(4L, app2, 2L, 3L)).thenReturn(participation2);

        AcceptedApplicationListResponse response = applicationService.acceptApplicationList(request);

        assertEquals(2, response.succeeded().size());
        assertTrue(response.failed().isEmpty());
        verify(emailSender).sendDecisionEmailList(any());
    }

    @Test
    void acceptApplicationList_duplicateIds_shouldThrowException() {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L, 1L));

        assertThrows(UnableToProcessApplicationException.class,
            () -> applicationService.acceptApplicationList(request));

        verify(applicationRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void acceptApplicationList_noApplicationsFound_shouldThrowApplicationNotFoundException() {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L, 2L));

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThrows(ApplicationNotFoundException.class,
            () -> applicationService.acceptApplicationList(request));
    }

    @Test
    void acceptApplicationList_applicationsBelongToDifferentStages_shouldThrowException() {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L, 2L));

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);

        Application app2 = new Application();
        app2.setId(2L);
        app2.setCompetitionId(2L);
        app2.setStageId(99L);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1, app2));

        assertThrows(UnableToProcessApplicationException.class,
            () -> applicationService.acceptApplicationList(request));
    }

    @Test
    void acceptApplicationList_someIdsNotFound_shouldAddToFailed() {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L, 99L));

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);
        app1.setIssuedBy(10L);
        app1.setStatus(RequestStatus.PENDING);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1));
        when(applicationSaver.saveAcceptedApplicationData(eq(4L), eq(app1), eq(2L), eq(3L)))
            .thenReturn(Participation.builder().userId(10L).competitionId(2L).stageId(3L).build());

        AcceptedApplicationListResponse response = applicationService.acceptApplicationList(request);

        assertEquals(1, response.succeeded().size());
        assertEquals(1, response.failed().size());
        assertEquals(99L, response.failed().getFirst().applicationId());
        assertEquals("Application not found", response.failed().getFirst().reason());
    }

    @Test
    void acceptApplicationList_applicationNotPending_shouldAddToFailed() {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L));

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);
        app1.setStatus(RequestStatus.ACCEPTED);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1));

        AcceptedApplicationListResponse response = applicationService.acceptApplicationList(request);

        assertTrue(response.succeeded().isEmpty());
        assertEquals("Application is not pending", response.failed().getFirst().reason());
        verify(applicationSaver, never()).saveAcceptedApplicationData(any(), any(), any(), any());
    }

    @Test
    void acceptApplicationList_participationConstraintViolation_shouldAddToFailed() {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L));

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);
        app1.setStatus(RequestStatus.PENDING);

        ConstraintViolationException constraintViolation = new ConstraintViolationException(
            "duplicate participation", null, "uc_participants_competition_id_stage_id");

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1));
        when(applicationSaver.saveAcceptedApplicationData(eq(4L), eq(app1), eq(2L), eq(3L)))
            .thenThrow(new DataIntegrityViolationException("constraint violation", constraintViolation));

        AcceptedApplicationListResponse response = applicationService.acceptApplicationList(request);

        assertTrue(response.succeeded().isEmpty());
        assertEquals("Application already has a participation record", response.failed().getFirst().reason());
    }

    @Test
    void acceptApplicationList_allFail_shouldNotScheduleEmail() {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L));

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);
        app1.setStatus(RequestStatus.ACCEPTED);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1));

        applicationService.acceptApplicationList(request);

        verify(emailSender, never()).sendDecisionEmailList(any());
    }

    // ---- rejectUserApplication ----

    @Test
    void rejectUserApplication_pendingApplication_shouldSetRejectedAndReason() {
        RejectEnrollmentRequest request = new RejectEnrollmentRequest(PROFILE_REJECTION_REASON);

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
        assertEquals(PROFILE_REJECTION_REASON, application.getRejectionReason());
        assertNotNull(application.getProcessedAt(), "Processed date should not be null");
        assertTrue(application.getProcessedAt()
            .isAfter(beforeMethod), "Date should be after the start of the test");
        assertTrue(application.getProcessedAt()
            .isBefore(Instant.now().plusSeconds(1)), "Date should not be in the future");
        verify(applicationRepository).saveAndFlush(application);
    }

    @Test
    void rejectUserApplication_applicationNotFound_shouldThrowApplicationNotFoundException() {
        RejectEnrollmentRequest request = new RejectEnrollmentRequest(PROFILE_REJECTION_REASON);
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class,
            () -> applicationService.rejectRequest(1L, request));

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectUserApplication_applicationNotPending_shouldThrowUnableToProcessApplicationException() {
        RejectEnrollmentRequest request = new RejectEnrollmentRequest(PROFILE_REJECTION_REASON);
        application.setStatus(RequestStatus.CANCELLED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(UnableToProcessApplicationException.class,
            () -> applicationService.rejectRequest(1L, request));

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    // ---- rejectApplicationList ----

    @Test
    void rejectApplicationList_validRequest_shouldRejectAllAndScheduleEmail() {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(1L, 2L), "Reason");

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);
        app1.setIssuedBy(10L);
        app1.setStatus(RequestStatus.PENDING);

        Application app2 = new Application();
        app2.setId(2L);
        app2.setCompetitionId(2L);
        app2.setStageId(3L);
        app2.setIssuedBy(11L);
        app2.setStatus(RequestStatus.PENDING);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1, app2));

        when(applicationSaver.saveRejectedApplication(4L, app1, "Reason")).thenReturn(app1);
        when(applicationSaver.saveRejectedApplication(4L, app2, "Reason")).thenReturn(app2);

        RejectedApplicationListResponse response = applicationService.rejectApplicationList(request);

        assertEquals(2, response.succeeded().size());
        assertTrue(response.failed().isEmpty());
        verify(emailSender).sendDecisionEmailList(any());
    }

    @Test
    void rejectApplicationList_duplicateIds_shouldThrowException() {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(1L, 1L), "Reason");

        assertThrows(UnableToProcessApplicationException.class,
            () -> applicationService.rejectApplicationList(request));

        verify(applicationRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void rejectApplicationList_noApplicationsFound_shouldThrowApplicationNotFoundException() {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(1L, 2L), "Reason");

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThrows(ApplicationNotFoundException.class,
            () -> applicationService.rejectApplicationList(request));
    }

    @Test
    void rejectApplicationList_applicationsBelongToDifferentStages_shouldThrowException() {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(1L, 2L), "Reason");

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);

        Application app2 = new Application();
        app2.setId(2L);
        app2.setCompetitionId(2L);
        app2.setStageId(99L);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1, app2));

        assertThrows(UnableToProcessApplicationException.class,
            () -> applicationService.rejectApplicationList(request));
    }

    @Test
    void rejectApplicationList_someIdsNotFound_shouldAddToFailed() {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(1L, 99L), "Reason");

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);
        app1.setIssuedBy(10L);
        app1.setStatus(RequestStatus.PENDING);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1));
        when(applicationSaver.saveRejectedApplication(eq(4L), eq(app1), any())).thenReturn(app1);

        RejectedApplicationListResponse response = applicationService.rejectApplicationList(request);

        assertEquals(1, response.succeeded().size());
        assertEquals(1, response.failed().size());
        assertEquals(99L, response.failed().getFirst().applicationId());
        assertEquals("Application not found", response.failed().getFirst().reason());
    }

    @Test
    void rejectApplicationList_applicationNotPending_shouldAddToFailed() {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(1L), "Reason");

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);
        app1.setStatus(RequestStatus.REJECTED);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1));

        RejectedApplicationListResponse response = applicationService.rejectApplicationList(request);

        assertTrue(response.succeeded().isEmpty());
        assertEquals("Application is not pending", response.failed().getFirst().reason());
        verify(applicationSaver, never()).saveRejectedApplication(any(), any(), any());
    }

    @Test
    void rejectApplicationList_allFail_shouldNotScheduleEmail() {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(1L), "Reason");

        Application app1 = new Application();
        app1.setId(1L);
        app1.setCompetitionId(2L);
        app1.setStageId(3L);
        app1.setStatus(RequestStatus.ACCEPTED);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(app1));

        applicationService.rejectApplicationList(request);

        verify(emailSender, never()).sendDecisionEmailList(any());
    }

    // ---- cancelUserApplication ----

    @Test
    void cancelUserApplication_ownPendingApplication_shouldCancel() {
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
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class,
            () -> applicationService.cancelRequest(1L));

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelUserApplication_applicationNotPending_shouldThrowUnableToProcessApplicationException() {
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

    // ---- getApplications ----

    @Test
    void getEnrollmentRequests_noCandidates_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of());

        Page<ApplicationListItemResponse> result = applicationService.getEnrollmentRequests(2L, 3L, null, pageable);

        assertTrue(result.isEmpty());
        verify(userFacade, never()).findUserIdsBySearchWithinIds(any(), any());
        verify(applicationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getEnrollmentRequests_noSearch_shouldReturnAllCandidates() {
        Pageable pageable = PageRequest.of(0, 10);

        Application candidateApplication = new Application();
        candidateApplication.setId(1L);
        candidateApplication.setIssuedBy(4L);
        candidateApplication.setIssuedAt(Instant.parse("2026-07-28T10:00:00Z"));
        candidateApplication.setStatus(RequestStatus.PENDING);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(candidateApplication));
        when(userFacade.findUserIdsBySearchWithinIds(null, List.of(4L))).thenReturn(Optional.empty());

        Page<Application> applicationPage = new PageImpl<>(List.of(candidateApplication), pageable, 1);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(applicationPage);

        UserProfileDetails user = new UserProfileDetails(4L, "Test", "Test Surname", "test@mail.com");
        when(userSummaryMapper.toUserSummary(user))
            .thenReturn(new UserSummary("Test", "Test Surname", "test@mail.com"));
        when(enrollmentAssembler.enrichWithUser(any(), any(), any())).thenAnswer(invocation -> {
            List<Application> apps = invocation.getArgument(0);
            BiFunction<Application, UserProfileDetails, ApplicationListItemResponse> combiner =
                invocation.getArgument(2);
            return apps.stream()
                .map(app -> combiner.apply(app, user))
                .toList();
        });
        Page<ApplicationListItemResponse> result = applicationService.getEnrollmentRequests(2L, 3L, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().getFirst().applicationId());
        assertEquals(RequestStatus.PENDING, result.getContent().getFirst().status());
    }

    @Test
    void getEnrollmentRequests_searchMatchesNoOne_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Application candidateApplication = new Application();
        candidateApplication.setIssuedBy(4L);

        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(candidateApplication));
        when(userFacade.findUserIdsBySearchWithinIds("xyz", List.of(4L))).thenReturn(Optional.of(List.of()));

        Page<ApplicationListItemResponse> result = applicationService.getEnrollmentRequests(2L, 3L, "xyz", pageable);

        assertTrue(result.isEmpty());
        verify(applicationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getEnrollmentRequests_stageDoesNotBelongToCompetition_shouldThrowCompetitionHierarchyValidationException() {
        Pageable pageable = PageRequest.of(0, 10);

        StageDetail mismatchedStage = StageDetail.builder()
            .id(3L)
            .competitionId(99L)
            .scope(StageScope.DISTRICT)
            .build();

        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(mismatchedStage));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> applicationService.getEnrollmentRequests(2L, 3L, null, pageable));

        assertTrue(exception.getMessage().contains("does not belong to this competition"));
        verify(applicationRepository, never()).findAll(any(Specification.class));
    }
}
