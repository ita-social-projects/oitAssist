package com.itasocialacademy.oitassist.chat.realtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.user.api.dto.ForumResponderCandidate;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationRealtimeRecipientResolverTest {

    private static final Long TASK_ASSIGNMENT_ID = 20L;
    private static final Long RESPONDER_ID = 200L;

    @Mock
    private TaskAssignmentForumResponderRepository responderRepository;

    @Mock
    private UserFacade userFacade;

    @InjectMocks
    private OrganizationRealtimeRecipientResolver resolver;

    @Test
    void isEligibleOrganizationResponder_grantedActiveOrgUser_shouldReturnTrue() {
        givenGrant(true);
        givenCandidate(Role.ORG, UserStatus.ACTIVE);

        assertTrue(resolver.isEligibleOrganizationResponder(TASK_ASSIGNMENT_ID, RESPONDER_ID));
    }

    @Test
    void isEligibleOrganizationResponder_grantedUserWithoutOrgRole_shouldReturnFalse() {
        givenGrant(true);
        givenCandidate(Role.USER, UserStatus.ACTIVE);

        assertFalse(resolver.isEligibleOrganizationResponder(TASK_ASSIGNMENT_ID, RESPONDER_ID));
    }

    @Test
    void isEligibleOrganizationResponder_grantedInactiveOrgUser_shouldReturnFalse() {
        givenGrant(true);
        givenCandidate(Role.ORG, UserStatus.BLOCKED);

        assertFalse(resolver.isEligibleOrganizationResponder(TASK_ASSIGNMENT_ID, RESPONDER_ID));
    }

    @Test
    void isEligibleOrganizationResponder_missingUser_shouldReturnFalse() {
        givenGrant(true);
        when(userFacade.findForumResponderCandidateById(RESPONDER_ID))
            .thenReturn(Optional.empty());

        assertFalse(resolver.isEligibleOrganizationResponder(TASK_ASSIGNMENT_ID, RESPONDER_ID));
    }

    @Test
    void isEligibleOrganizationResponder_withoutGrant_shouldReturnFalseWithoutLoadingUser() {
        givenGrant(false);

        assertFalse(resolver.isEligibleOrganizationResponder(TASK_ASSIGNMENT_ID, RESPONDER_ID));

        verifyNoInteractions(userFacade);
    }

    @Test
    void isEligibleOrganizationResponder_unassignedQuestion_shouldReturnFalseWithoutQueries() {
        assertFalse(resolver.isEligibleOrganizationResponder(TASK_ASSIGNMENT_ID, null));

        verifyNoInteractions(
            responderRepository,
            userFacade);
    }

    private void givenGrant(boolean granted) {
        when(responderRepository.existsByTaskAssignmentIdAndResponderUserId(TASK_ASSIGNMENT_ID, RESPONDER_ID))
            .thenReturn(granted);
    }

    private void givenCandidate(Role role, UserStatus status) {
        when(userFacade.findForumResponderCandidateById(RESPONDER_ID))
            .thenReturn(Optional.of(new ForumResponderCandidate(
                RESPONDER_ID,
                "responder@example.com",
                "Olena",
                "Koval",
                role,
                status)));
    }
}
