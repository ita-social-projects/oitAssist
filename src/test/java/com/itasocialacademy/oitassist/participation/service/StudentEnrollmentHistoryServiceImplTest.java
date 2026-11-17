package com.itasocialacademy.oitassist.participation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
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
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class StudentEnrollmentHistoryServiceImplTest {
    private static final Long USER_ID = 4L;
    private static final Long COMPETITION_ID = 10L;
    private static final Long STAGE_ID = 20L;
    private static final String ORG_ROLE = "ORG";

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private CompetitionFacade competitionFacade;

    @InjectMocks
    private StudentEnrollmentHistoryServiceImpl service;

    private Pageable pageable;
    private CompetitionSummary competitionSummary;
    private StageSummary stageSummary;
    private CompetitionDetail competitionDetail;
    private StageDetail stageDetail;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        competitionSummary = new CompetitionSummary(COMPETITION_ID, "Olympiad");
        stageSummary = new StageSummary(STAGE_ID, "City stage", StageScope.CITY);
        competitionDetail = CompetitionDetail.builder().id(COMPETITION_ID).title("Olympiad").build();
        stageDetail = StageDetail.builder().id(STAGE_ID).title("City stage").scope(StageScope.CITY).build();
    }

    @Test
    void getInvitations_owner_shouldReturnPageWithCompetitionAndStage() {
        Instant issuedAt = Instant.parse("2026-06-07T09:50:30Z");
        Invitation invitation = Invitation.builder()
            .id(1L)
            .competitionId(COMPETITION_ID)
            .stageId(STAGE_ID)
            .issuedAt(issuedAt)
            .status(RequestStatus.PENDING)
            .build();
        when(securityFacade.isOwner(USER_ID)).thenReturn(true);
        when(invitationRepository.findAll(ArgumentMatchers.<Specification<Invitation>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(invitation), pageable, 1));
        when(competitionFacade.findCompetitionsByIds(Set.of(COMPETITION_ID))).thenReturn(List.of(competitionDetail));
        when(competitionFacade.findStagesByIds(Set.of(STAGE_ID))).thenReturn(List.of(stageDetail));

        Page<InvitationHistoryItemResponse> result = service.getInvitations(USER_ID, null, pageable);

        assertEquals(
            List.of(new InvitationHistoryItemResponse(
                1L, issuedAt, RequestStatus.PENDING, competitionSummary, stageSummary)),
            result.getContent());
    }

    @Test
    void getApplications_organizer_shouldReturnPageWithCompetitionAndStage() {
        Instant issuedAt = Instant.parse("2026-06-07T09:50:30Z");
        Application application = Application.builder()
            .id(2L)
            .competitionId(COMPETITION_ID)
            .stageId(STAGE_ID)
            .issuedAt(issuedAt)
            .status(RequestStatus.ACCEPTED)
            .build();
        when(securityFacade.isOwner(USER_ID)).thenReturn(false);
        when(securityFacade.hasRole(ORG_ROLE)).thenReturn(true);
        when(applicationRepository.findAll(ArgumentMatchers.<Specification<Application>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(application), pageable, 1));
        when(competitionFacade.findCompetitionsByIds(Set.of(COMPETITION_ID))).thenReturn(List.of(competitionDetail));
        when(competitionFacade.findStagesByIds(Set.of(STAGE_ID))).thenReturn(List.of(stageDetail));

        Page<ApplicationHistoryItemResponse> result =
            service.getApplications(USER_ID, RequestStatus.ACCEPTED, pageable);

        assertEquals(
            List.of(new ApplicationHistoryItemResponse(
                2L, issuedAt, RequestStatus.ACCEPTED, competitionSummary, stageSummary)),
            result.getContent());
    }

    @Test
    void getParticipations_owner_shouldReturnPageWithCompetitionAndStage() {
        Participation participation = Participation.builder()
            .id(3L)
            .competitionId(COMPETITION_ID)
            .stageId(STAGE_ID)
            .build();
        when(securityFacade.isOwner(USER_ID)).thenReturn(true);
        when(participationRepository.findAll(ArgumentMatchers.<Specification<Participation>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(participation), pageable, 1));
        when(competitionFacade.findCompetitionsByIds(Set.of(COMPETITION_ID))).thenReturn(List.of(competitionDetail));
        when(competitionFacade.findStagesByIds(Set.of(STAGE_ID))).thenReturn(List.of(stageDetail));

        Page<ParticipationHistoryItemResponse> result = service.getParticipations(USER_ID, pageable);

        assertEquals(
            List.of(new ParticipationHistoryItemResponse(3L, competitionSummary, stageSummary)),
            result.getContent());
    }

    @Test
    void getInvitations_notOwnerNotOrganizer_shouldThrowInsufficientPermissionsException() {
        when(securityFacade.isOwner(USER_ID)).thenReturn(false);
        when(securityFacade.hasRole(ORG_ROLE)).thenReturn(false);

        assertThrows(InsufficientPermissionsException.class,
            () -> service.getInvitations(USER_ID, null, pageable));

        verifyNoInteractions(invitationRepository, competitionFacade);
    }

    @Test
    void getApplications_notOwnerNotOrganizer_shouldThrowInsufficientPermissionsException() {
        when(securityFacade.isOwner(USER_ID)).thenReturn(false);
        when(securityFacade.hasRole(ORG_ROLE)).thenReturn(false);

        assertThrows(InsufficientPermissionsException.class,
            () -> service.getApplications(USER_ID, null, pageable));

        verifyNoInteractions(applicationRepository, competitionFacade);
    }

    @Test
    void getParticipations_notOwnerNotOrganizer_shouldThrowInsufficientPermissionsException() {
        when(securityFacade.isOwner(USER_ID)).thenReturn(false);
        when(securityFacade.hasRole(ORG_ROLE)).thenReturn(false);

        assertThrows(InsufficientPermissionsException.class,
            () -> service.getParticipations(USER_ID, pageable));

        verifyNoInteractions(participationRepository, competitionFacade);
    }

    @Test
    void getInvitations_stageNotFound_shouldReturnItemWithCompetitionAndNullStage() {
        Instant issuedAt = Instant.parse("2026-06-07T09:50:30Z");
        Invitation invitation = Invitation.builder()
            .id(1L)
            .competitionId(COMPETITION_ID)
            .stageId(STAGE_ID)
            .issuedAt(issuedAt)
            .status(RequestStatus.PENDING)
            .build();
        when(securityFacade.isOwner(USER_ID)).thenReturn(true);
        when(invitationRepository.findAll(ArgumentMatchers.<Specification<Invitation>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(invitation), pageable, 1));
        when(competitionFacade.findCompetitionsByIds(Set.of(COMPETITION_ID))).thenReturn(List.of(competitionDetail));
        when(competitionFacade.findStagesByIds(Set.of(STAGE_ID))).thenReturn(List.of());

        Page<InvitationHistoryItemResponse> result = service.getInvitations(USER_ID, null, pageable);

        assertEquals(
            List.of(new InvitationHistoryItemResponse(
                1L, issuedAt, RequestStatus.PENDING, competitionSummary, null)),
            result.getContent());
    }
}