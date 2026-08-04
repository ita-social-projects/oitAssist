package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderGrantResult;
import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderResponseDTO;
import com.itasocialacademy.oitassist.chat.exceptions.ForumResponderActiveReviewException;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidForumResponderCandidateException;
import com.itasocialacademy.oitassist.chat.service.interfaces.TaskAssignmentForumResponderService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;

class AdministratorForumResponderControllerTest
    extends ControllerUnitTest<AdministratorForumResponderController> {

    private static final Long TASK_ASSIGNMENT_ID = 10L;
    private static final Long RESPONDER_ID = 20L;
    private static final Long ADMINISTRATOR_ID = 30L;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private static final Instant ASSIGNED_AT =
        Instant.parse("2026-08-04T12:00:00Z");

    private static final String ROOT_URL =
        "/api/v1/admin/task-assignments";

    private static final String BASE_URL =
        ROOT_URL
            + "/"
            + TASK_ASSIGNMENT_ID
            + "/forum-responders";

    @Mock
    private TaskAssignmentForumResponderService responderService;

    @InjectMocks
    private AdministratorForumResponderController controller;

    @Override
    protected AdministratorForumResponderController getController() {

        return controller;
    }

    /*
     * Controller contract
     */

    @Test
    void controller_shouldRequireGlobalAdministratorRole() {

        PreAuthorize annotation =
            AdministratorForumResponderController.class
                .getAnnotation(PreAuthorize.class);

        assertNotNull(annotation);

        assertEquals(
            "hasRole('ADMIN')",
            annotation.value());
    }

    /*
     * List responders
     */

    @Test
    void getResponders_defaultPagination_shouldReturnPage()
        throws Exception {

        Page<TaskAssignmentForumResponderResponseDTO> page =
            new PageImpl<>(
                List.of(createResponse()),
                PageRequest.of(
                    DEFAULT_PAGE,
                    DEFAULT_SIZE),
                1);

        when(responderService.getResponders(
            TASK_ASSIGNMENT_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE))
            .thenReturn(page);

        mockMvc.perform(
            get(BASE_URL))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content")
                    .isArray())
            .andExpect(
                jsonPath("$.content.length()")
                    .value(1))
            .andExpect(
                jsonPath("$.content[0].id")
                    .value(1L))
            .andExpect(
                jsonPath("$.content[0].taskAssignmentId")
                    .value(TASK_ASSIGNMENT_ID))
            .andExpect(
                jsonPath("$.content[0].responderUserId")
                    .value(RESPONDER_ID))
            .andExpect(
                jsonPath("$.content[0].responderEmail")
                    .value("org@example.com"))
            .andExpect(
                jsonPath("$.content[0].responderFirstName")
                    .value("Olena"))
            .andExpect(
                jsonPath("$.content[0].responderLastName")
                    .value("Koval"))
            .andExpect(
                jsonPath("$.content[0].assignedByUserId")
                    .value(ADMINISTRATOR_ID))
            .andExpect(
                jsonPath("$.content[0].assignedAt")
                    .value(ASSIGNED_AT.toString()))
            .andExpect(
                jsonPath("$.pageNumber")
                    .value(DEFAULT_PAGE))
            .andExpect(
                jsonPath("$.pageSize")
                    .value(DEFAULT_SIZE))
            .andExpect(
                jsonPath("$.totalPages")
                    .value(1))
            .andExpect(
                jsonPath("$.totalElements")
                    .value(1))
            .andExpect(
                jsonPath("$.first")
                    .value(true))
            .andExpect(
                jsonPath("$.last")
                    .value(true));

        verify(responderService).getResponders(
            TASK_ASSIGNMENT_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE);
    }

    @Test
    void getResponders_explicitPagination_shouldDelegateExactValues()
        throws Exception {

        Page<TaskAssignmentForumResponderResponseDTO> page =
            new PageImpl<>(
                List.of(createResponse()),
                PageRequest.of(2, 15),
                31);

        when(responderService.getResponders(
            TASK_ASSIGNMENT_ID,
            2,
            15))
            .thenReturn(page);

        mockMvc.perform(
            get(BASE_URL)
                .param("page", "2")
                .param("size", "15"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.pageNumber")
                    .value(2))
            .andExpect(
                jsonPath("$.pageSize")
                    .value(15))
            .andExpect(
                jsonPath("$.totalElements")
                    .value(31));

        verify(responderService).getResponders(
            TASK_ASSIGNMENT_ID,
            2,
            15);
    }

    @Test
    void getResponders_emptyResult_shouldReturnEmptyPage()
        throws Exception {

        when(responderService.getResponders(
            TASK_ASSIGNMENT_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        DEFAULT_PAGE,
                        DEFAULT_SIZE)));

        mockMvc.perform(
            get(BASE_URL))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content")
                    .isEmpty())
            .andExpect(
                jsonPath("$.totalElements")
                    .value(0))
            .andExpect(
                jsonPath("$.first")
                    .value(true))
            .andExpect(
                jsonPath("$.last")
                    .value(true));
    }

    @Test
    void getResponders_negativePage_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            get(BASE_URL)
                .param("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void getResponders_zeroSize_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            get(BASE_URL)
                .param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void getResponders_sizeAboveMaximum_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            get(BASE_URL)
                .param(
                    "size",
                    String.valueOf(
                        MAX_PAGE_SIZE + 1)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void getResponders_nonnumericPage_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            get(BASE_URL)
                .param("page", "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void getResponders_invalidTaskAssignmentId_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            get(
                ROOT_URL
                    + "/0/forum-responders"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void getResponders_nonnumericTaskAssignmentId_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            get(
                ROOT_URL
                    + "/invalid/forum-responders"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void getResponders_missingTaskAssignment_shouldReturn404()
        throws Exception {

        when(responderService.getResponders(
            TASK_ASSIGNMENT_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE))
            .thenThrow(
                new TaskAssignmentNotFoundException(
                    TASK_ASSIGNMENT_ID));

        mockMvc.perform(
            get(BASE_URL))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "TASK_ASSIGNMENT_NOT_FOUND"));
    }

    @Test
    void getResponders_unauthenticated_shouldReturn401()
        throws Exception {

        when(responderService.getResponders(
            TASK_ASSIGNMENT_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE))
            .thenThrow(authenticationException());

        mockMvc.perform(
            get(BASE_URL))
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "AUTHENTICATION_REQUIRED"));
    }

    @Test
    void getResponders_nonAdministrator_shouldReturn403()
        throws Exception {

        when(responderService.getResponders(
            TASK_ASSIGNMENT_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE))
            .thenThrow(authorizationException());

        mockMvc.perform(
            get(BASE_URL))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "ACCESS_DENIED"));
    }

    /*
     * Grant responder
     */

    @Test
    void grantResponder_created_shouldReturn201()
        throws Exception {

        TaskAssignmentForumResponderResponseDTO response =
            createResponse();

        when(responderService.grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID))
            .thenReturn(
                new TaskAssignmentForumResponderGrantResult(
                    true,
                    response));

        mockMvc.perform(
            put(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.id")
                    .value(1L))
            .andExpect(
                jsonPath("$.taskAssignmentId")
                    .value(TASK_ASSIGNMENT_ID))
            .andExpect(
                jsonPath("$.responderUserId")
                    .value(RESPONDER_ID))
            .andExpect(
                jsonPath("$.responderEmail")
                    .value("org@example.com"))
            .andExpect(
                jsonPath("$.assignedByUserId")
                    .value(ADMINISTRATOR_ID));

        verify(responderService).grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);
    }

    @Test
    void grantResponder_existingAssignment_shouldReturn200()
        throws Exception {

        when(responderService.grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID))
            .thenReturn(
                new TaskAssignmentForumResponderGrantResult(
                    false,
                    createResponse()));

        mockMvc.perform(
            put(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.responderUserId")
                    .value(RESPONDER_ID));

        verify(responderService).grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);
    }

    @Test
    void grantResponder_invalidTaskAssignmentId_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            put(
                ROOT_URL
                    + "/0/forum-responders/"
                    + RESPONDER_ID))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void grantResponder_invalidUserId_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            put(
                BASE_URL
                    + "/0"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void grantResponder_nonnumericUserId_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            put(
                BASE_URL
                    + "/invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void grantResponder_missingTaskAssignment_shouldReturn404()
        throws Exception {

        when(responderService.grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID))
            .thenThrow(
                new TaskAssignmentNotFoundException(
                    TASK_ASSIGNMENT_ID));

        mockMvc.perform(
            put(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "TASK_ASSIGNMENT_NOT_FOUND"));
    }

    @Test
    void grantResponder_missingUser_shouldReturn404()
        throws Exception {

        when(responderService.grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID))
            .thenThrow(userNotFoundException());

        mockMvc.perform(
            put(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value("USER_NOT_FOUND"));
    }

    @Test
    void grantResponder_nonOrgCandidate_shouldReturn400()
        throws Exception {

        when(responderService.grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID))
            .thenThrow(
                new InvalidForumResponderCandidateException(
                    RESPONDER_ID,
                    Role.USER,
                    UserStatus.ACTIVE));

        mockMvc.perform(
            put(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "FORUM_RESPONDER_INVALID"));
    }

    @Test
    void grantResponder_inactiveOrgCandidate_shouldReturn400()
        throws Exception {

        when(responderService.grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID))
            .thenThrow(
                new InvalidForumResponderCandidateException(
                    RESPONDER_ID,
                    Role.ORG,
                    UserStatus.INACTIVE));

        mockMvc.perform(
            put(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "FORUM_RESPONDER_INVALID"));
    }

    @Test
    void grantResponder_unauthenticated_shouldReturn401()
        throws Exception {

        when(responderService.grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID))
            .thenThrow(authenticationException());

        mockMvc.perform(
            put(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "AUTHENTICATION_REQUIRED"));
    }

    @Test
    void grantResponder_userCaller_shouldReturn403()
        throws Exception {

        when(responderService.grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID))
            .thenThrow(authorizationException());

        mockMvc.perform(
            put(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "ACCESS_DENIED"));
    }

    @Test
    void grantResponder_orgCaller_shouldReturn403()
        throws Exception {

        when(responderService.grantResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID))
            .thenThrow(authorizationException());

        mockMvc.perform(
            put(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "ACCESS_DENIED"));
    }

    /*
     * Revoke responder
     */

    @Test
    void revokeResponder_existingAssignment_shouldReturn204()
        throws Exception {

        mockMvc.perform(
            delete(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(responderService).revokeResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);
    }

    @Test
    void revokeResponder_missingAssignmentRecord_shouldReturn204()
        throws Exception {

        mockMvc.perform(
            delete(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isNoContent());

        verify(responderService).revokeResponder(
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID);
    }

    @Test
    void revokeResponder_invalidTaskAssignmentId_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            delete(
                ROOT_URL
                    + "/0/forum-responders/"
                    + RESPONDER_ID))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void revokeResponder_invalidUserId_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            delete(
                BASE_URL
                    + "/0"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(responderService);
    }

    @Test
    void revokeResponder_missingTaskAssignment_shouldReturn404()
        throws Exception {

        doThrow(
            new TaskAssignmentNotFoundException(
                TASK_ASSIGNMENT_ID))
            .when(responderService)
            .revokeResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        mockMvc.perform(
            delete(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "TASK_ASSIGNMENT_NOT_FOUND"));
    }

    @Test
    void revokeResponder_missingUser_shouldReturn404()
        throws Exception {

        doThrow(userNotFoundException())
            .when(responderService)
            .revokeResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        mockMvc.perform(
            delete(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value("USER_NOT_FOUND"));
    }

    @Test
    void revokeResponder_activeReview_shouldReturn409()
        throws Exception {

        doThrow(
            new ForumResponderActiveReviewException(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .when(responderService)
            .revokeResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        mockMvc.perform(
            delete(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "FORUM_RESPONDER_ACTIVE_REVIEW_CONFLICT"));
    }

    @Test
    void revokeResponder_unauthenticated_shouldReturn401()
        throws Exception {

        doThrow(authenticationException())
            .when(responderService)
            .revokeResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        mockMvc.perform(
            delete(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "AUTHENTICATION_REQUIRED"));
    }

    @Test
    void revokeResponder_nonAdministrator_shouldReturn403()
        throws Exception {

        doThrow(authorizationException())
            .when(responderService)
            .revokeResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        mockMvc.perform(
            delete(
                BASE_URL
                    + "/"
                    + RESPONDER_ID))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "ACCESS_DENIED"));
    }

    /*
     * Helpers
     */

    private TaskAssignmentForumResponderResponseDTO createResponse() {

        return new TaskAssignmentForumResponderResponseDTO(
            1L,
            TASK_ASSIGNMENT_ID,
            RESPONDER_ID,
            "org@example.com",
            "Olena",
            "Koval",
            ADMINISTRATOR_ID,
            ASSIGNED_AT);
    }

    private AuthenticationException authenticationException() {

        return new AuthenticationException(
            "Authentication is required to manage "
                + "TaskAssignment forum responders",
            ErrorCode.AUTHENTICATION_REQUIRED);
    }

    private AuthorizationException authorizationException() {

        return new AuthorizationException(
            "Global administrator role is required "
                + "to manage TaskAssignment forum responders",
            ErrorCode.ACCESS_DENIED);
    }

    private NotFoundException userNotFoundException() {

        return new NotFoundException(
            "User with id %s was not found"
                .formatted(RESPONDER_ID),
            ErrorCode.USER_NOT_FOUND);
    }
}