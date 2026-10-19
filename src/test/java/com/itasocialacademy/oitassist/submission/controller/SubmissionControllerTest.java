package com.itasocialacademy.oitassist.submission.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.submission.dao.dto.response.SubmissionResponseDTO;
import com.itasocialacademy.oitassist.submission.exceptions.SubmissionNotFoundException;
import com.itasocialacademy.oitassist.submission.service.interfaces.SubmissionService;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;

class SubmissionControllerTest extends ControllerUnitTest<SubmissionController> {

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private SubmissionController submissionController;

    private SubmissionResponseDTO mockSubmissionResponse;

    @Override
    protected SubmissionController getController() {
        return submissionController;
    }

    @BeforeEach
    void setUpMockData() {
        mockSubmissionResponse = SubmissionResponseDTO.builder()
            .id(1L)
            .taskAssignmentId(10L)
            .submittedBy(100L)
            .comment("Test submission")
            .build();
    }

    @Test
    @DisplayName("getSubmissionById should return submission when id is valid")
    void getSubmissionById_ValidId_ShouldReturn200() throws Exception {
        when(submissionService.getSubmissionById(1L))
            .thenReturn(mockSubmissionResponse);

        mockMvc.perform(get("/api/v1/submissions/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.taskAssignmentId").value(10L))
            .andExpect(jsonPath("$.submittedBy").value(100L))
            .andExpect(jsonPath("$.comment").value("Test submission"));

        verify(submissionService).getSubmissionById(1L);
    }

    @Test
    @DisplayName("getSubmissionById should return 404 when submission is not found")
    void getSubmissionById_SubmissionNotFound_ShouldReturn404() throws Exception {
        when(submissionService.getSubmissionById(1L))
            .thenThrow(new SubmissionNotFoundException(1L));

        mockMvc.perform(get("/api/v1/submissions/{id}", 1L))
            .andExpect(status().isNotFound());

        verify(submissionService).getSubmissionById(1L);
    }

    @Test
    @DisplayName("getSubmissionById should return 403 when user has insufficient permissions")
    void getSubmissionById_InsufficientPermissions_ShouldReturn403() throws Exception {
        when(submissionService.getSubmissionById(1L))
            .thenThrow(new InsufficientPermissionsException());

        mockMvc.perform(get("/api/v1/submissions/{id}", 1L))
            .andExpect(status().isForbidden());

        verify(submissionService).getSubmissionById(1L);
    }

    @Test
    @DisplayName("getSubmissionByUserIdAndTaskAssignmentId should return submission when parameters are valid")
    void getSubmissionByUserIdAndTaskAssignmentId_ValidRequest_ShouldReturn200() throws Exception {
        when(submissionService.getSubmissionBySubmittedByAndTaskAssignmentId(100L, 10L))
            .thenReturn(mockSubmissionResponse);

        mockMvc.perform(get(
            "/api/v1/submissions/{userId}/{taskAssignmentId}/by-user-and-task-assignment",
            100L,
            10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.taskAssignmentId").value(10L))
            .andExpect(jsonPath("$.submittedBy").value(100L));

        verify(submissionService)
            .getSubmissionBySubmittedByAndTaskAssignmentId(100L, 10L);
    }

    @Test
    @DisplayName("getSubmissionByUserIdAndTaskAssignmentId should return 404 when submission is not found")
    void getSubmissionByUserIdAndTaskAssignmentId_SubmissionNotFound_ShouldReturn404() throws Exception {
        when(submissionService.getSubmissionBySubmittedByAndTaskAssignmentId(100L, 10L))
            .thenThrow(new SubmissionNotFoundException(100L, 10L));

        mockMvc.perform(get(
            "/api/v1/submissions/{userId}/{taskAssignmentId}/by-user-and-task-assignment",
            100L,
            10L))
            .andExpect(status().isNotFound());

        verify(submissionService)
            .getSubmissionBySubmittedByAndTaskAssignmentId(100L, 10L);
    }

    @Test
    @DisplayName("getSubmissionByUserIdAndTaskAssignmentId should return 403 when user has insufficient permissions")
    void getSubmissionByUserIdAndTaskAssignmentId_InsufficientPermissions_ShouldReturn403()
        throws Exception {
        when(submissionService.getSubmissionBySubmittedByAndTaskAssignmentId(100L, 10L))
            .thenThrow(new InsufficientPermissionsException());

        mockMvc.perform(get(
            "/api/v1/submissions/{userId}/{taskAssignmentId}/by-user-and-task-assignment",
            100L,
            10L))
            .andExpect(status().isForbidden());

        verify(submissionService)
            .getSubmissionBySubmittedByAndTaskAssignmentId(100L, 10L);
    }

    @Test
    @DisplayName("getMySubmissionByTaskAssignmentId should return submission when request is valid")
    void getMySubmissionByTaskAssignmentId_ValidRequest_ShouldReturn200() throws Exception {
        when(submissionService.getMySubmissionByTaskAssignmentId(10L))
            .thenReturn(mockSubmissionResponse);

        mockMvc.perform(get(
            "/api/v1/submissions/my/{taskAssignmentId}/by-task-assignment",
            10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.taskAssignmentId").value(10L))
            .andExpect(jsonPath("$.submittedBy").value(100L))
            .andExpect(jsonPath("$.comment").value("Test submission"));

        verify(submissionService).getMySubmissionByTaskAssignmentId(10L);
    }

    @Test
    @DisplayName("getMySubmissionByTaskAssignmentId should return 404 when submission is not found")
    void getMySubmissionByTaskAssignmentId_SubmissionNotFound_ShouldReturn404() throws Exception {
        when(submissionService.getMySubmissionByTaskAssignmentId(10L))
            .thenThrow(new SubmissionNotFoundException(100L, 10L));

        mockMvc.perform(get(
            "/api/v1/submissions/my/{taskAssignmentId}/by-task-assignment",
            10L))
            .andExpect(status().isNotFound());

        verify(submissionService).getMySubmissionByTaskAssignmentId(10L);
    }

    @Test
    @DisplayName("postSubmission should create submission when request is valid")
    void postSubmission_ValidRequest_ShouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "solution.cpp",
            "text/plain",
            "test content".getBytes());

        when(submissionService.createSubmission(
            eq("Test comment"),
            eq(10L),
            anyList())).thenReturn(mockSubmissionResponse);

        mockMvc.perform(multipart("/api/v1/submissions")
            .file(file)
            .param("comment", "Test comment")
            .param("taskAssignmentId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.taskAssignmentId").value(10L))
            .andExpect(jsonPath("$.submittedBy").value(100L))
            .andExpect(jsonPath("$.comment").value("Test submission"));

        verify(submissionService).createSubmission(
            eq("Test comment"),
            eq(10L),
            anyList());
    }

    @Test
    @DisplayName("postSubmission should create submission when comment is not provided")
    void postSubmission_WithoutComment_ShouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "solution.cpp",
            "text/plain",
            "test content".getBytes());

        when(submissionService.createSubmission(
            isNull(),
            eq(10L),
            anyList())).thenReturn(mockSubmissionResponse);

        mockMvc.perform(multipart("/api/v1/submissions")
            .file(file)
            .param("taskAssignmentId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L));

        verify(submissionService).createSubmission(
            isNull(),
            eq(10L),
            anyList());
    }

    @Test
    @DisplayName("postSubmission should return 400 when files are not provided")
    void postSubmission_WithoutFiles_ShouldReturn400() throws Exception {
        mockMvc.perform(multipart("/api/v1/submissions")
            .param("comment", "Test comment")
            .param("taskAssignmentId", "10"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(submissionService);
    }

    @Test
    @DisplayName("postSubmission should return 400 when task assignment id is not provided")
    void postSubmission_WithoutTaskAssignmentId_ShouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "solution.cpp",
            "text/plain",
            "test content".getBytes());

        mockMvc.perform(multipart("/api/v1/submissions")
            .file(file)
            .param("comment", "Test comment"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(submissionService);
    }

    @Test
    @DisplayName("postSubmission should return 404 when task assignment is not found")
    void postSubmission_TaskAssignmentNotFound_ShouldReturn404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "solution.cpp",
            "text/plain",
            "test content".getBytes());

        when(submissionService.createSubmission(
            eq("Test comment"),
            eq(10L),
            anyList())).thenThrow(new TaskAssignmentNotFoundException(10L));

        mockMvc.perform(multipart("/api/v1/submissions")
            .file(file)
            .param("comment", "Test comment")
            .param("taskAssignmentId", "10"))
            .andExpect(status().isNotFound());

        verify(submissionService).createSubmission(
            eq("Test comment"),
            eq(10L),
            anyList());
    }
}