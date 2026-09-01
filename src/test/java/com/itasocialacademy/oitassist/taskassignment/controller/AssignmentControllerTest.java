package com.itasocialacademy.oitassist.taskassignment.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskRequirements;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateAndAssignTaskRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.TaskRequirementsRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.UpdateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.DetailedTaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.LinkedToursResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAlreadyAssignedException;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import com.itasocialacademy.oitassist.taskassignment.service.interfaces.AssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AssignmentControllerTest extends ControllerUnitTest<AssignmentController> {
    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private AssignmentController assignmentController;

    private TaskAssignmentResponseDTO mockAssignmentResponse;
    private DetailedTaskAssignmentResponseDTO mockDetailedResponse;
    private TaskRequirementsRequestDTO validRequirements;

    @Override
    protected AssignmentController getController() {
        return assignmentController;
    }

    @BeforeEach
    void setUp() {
        validRequirements = new TaskRequirementsRequestDTO(List.of(
            new TaskRequirementsRequestDTO.RequiredFileRequest("Файл розв'язку", "PowerPoint_РіздвянаЗірка",
                List.of(".pptx"), 50)));

        TaskRequirements requirements = new TaskRequirements(List.of(
            new TaskRequirements.RequiredFile("Файл розв'язку", "PowerPoint_РіздвянаЗірка",
                List.of(".pptx"), 50)));

        List<FileDetailsDTO> testFiles = List.of(
            new FileDetailsDTO(1L, "problem.pdf", "application/pdf", 2048L, "PROBLEM",
                "/uploads/task/problem.pdf"));

        mockAssignmentResponse = TaskAssignmentResponseDTO.builder()
            .id(1L)
            .taskBodyId(3L)
            .taskTitle("PowerPoint Різдвяна зірка")
            .tourId(10L)
            .maxPoints(25)
            .createdBy(100L)
            .build();

        mockDetailedResponse = new DetailedTaskAssignmentResponseDTO(
            1L, 3L, "PowerPoint Різдвяна зірка", "Створити у файлі-розв'язку",
            10L, AssignmentVisibility.VISIBLE, 25, requirements, testFiles, 100L, 0L);
    }

    // POST /api/v1/tours/{tourId}/task-assignments — assignTask

    @Test
    void assignTask_validRequest_shouldReturn201() throws Exception {
        CreateTaskAssignmentRequestDTO request = new CreateTaskAssignmentRequestDTO(
            3L, AssignmentVisibility.VISIBLE, 25, validRequirements);

        when(assignmentService.assignTask(eq(10L), any(CreateTaskAssignmentRequestDTO.class)))
            .thenReturn(mockDetailedResponse);

        mockMvc.perform(post("/api/v1/tours/{tourId}/task-assignments", 10L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(mockDetailedResponse.id()))
            .andExpect(jsonPath("$.taskBodyId").value(mockDetailedResponse.taskBodyId()))
            .andExpect(jsonPath("$.tourId").value(mockDetailedResponse.tourId()))
            .andExpect(jsonPath("$.taskTitle").value(mockDetailedResponse.taskTitle()))
            .andExpect(jsonPath("$.taskDescription").value(mockDetailedResponse.taskDescription()))
            .andExpect(jsonPath("$.visibility").value(mockDetailedResponse.visibility().name()))
            .andExpect(jsonPath("$.files").isArray());
    }

    @Test
    void assignTask_tourNotFound_shouldReturn404() throws Exception {
        CreateTaskAssignmentRequestDTO request = new CreateTaskAssignmentRequestDTO(
            3L, AssignmentVisibility.VISIBLE, 25, validRequirements);

        when(assignmentService.assignTask(eq(99L), any(CreateTaskAssignmentRequestDTO.class)))
            .thenThrow(new TourNotFoundException(99L));

        mockMvc.perform(post("/api/v1/tours/{tourId}/task-assignments", 99L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void assignTask_tourNotScheduled_shouldReturn400() throws Exception {
        CreateTaskAssignmentRequestDTO request = new CreateTaskAssignmentRequestDTO(
            3L, AssignmentVisibility.VISIBLE, 25, validRequirements);

        when(assignmentService.assignTask(eq(10L), any(CreateTaskAssignmentRequestDTO.class)))
            .thenThrow(new CompetitionHierarchyValidationException("Cannot assign task. Tour has already started"));

        mockMvc.perform(post("/api/v1/tours/{tourId}/task-assignments", 10L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void assignTask_duplicateAssignment_shouldReturn409() throws Exception {
        CreateTaskAssignmentRequestDTO request = new CreateTaskAssignmentRequestDTO(
            3L, AssignmentVisibility.VISIBLE, 25, validRequirements);

        when(assignmentService.assignTask(eq(10L), any(CreateTaskAssignmentRequestDTO.class)))
            .thenThrow(new TaskAlreadyAssignedException(3L, 10L));

        mockMvc.perform(post("/api/v1/tours/{tourId}/task-assignments", 10L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void assignTask_nullTaskBodyId_shouldReturn400() throws Exception {
        CreateTaskAssignmentRequestDTO request = new CreateTaskAssignmentRequestDTO(
            null, AssignmentVisibility.VISIBLE, null, null);

        mockMvc.perform(post("/api/v1/tours/{tourId}/task-assignments", 10L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(assignmentService);
    }

    // POST /api/v1/tours/{tourId}/task-assignments/new — createAndAssignTask

    @Test
    void createAndAssignTask_validRequest_shouldReturn201() throws Exception {
        CreateAndAssignTaskRequestDTO metadata = new CreateAndAssignTaskRequestDTO(
            "Task Title", "Task Description", AssignmentVisibility.VISIBLE, 25, validRequirements);

        MockMultipartFile metadataPart = new MockMultipartFile(
            "metadata", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(metadata));

        when(assignmentService.createAndAssignTask(eq(10L), any(), any(), any(), any()))
            .thenReturn(mockDetailedResponse);

        mockMvc.perform(multipart("/api/v1/tours/{tourId}/task-assignments/new", 10L)
            .file(metadataPart))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(mockDetailedResponse.id()))
            .andExpect(jsonPath("$.taskBodyId").value(mockDetailedResponse.taskBodyId()))
            .andExpect(jsonPath("$.tourId").value(mockDetailedResponse.tourId()))
            .andExpect(jsonPath("$.taskTitle").value(mockDetailedResponse.taskTitle()));
    }

    @Test
    void createAndAssignTask_tourNotFound_shouldReturn404() throws Exception {
        CreateAndAssignTaskRequestDTO metadata = new CreateAndAssignTaskRequestDTO(
            "Task Title", "Task Description", AssignmentVisibility.VISIBLE, 25, validRequirements);

        MockMultipartFile metadataPart = new MockMultipartFile(
            "metadata", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(metadata));

        when(assignmentService.createAndAssignTask(eq(99L), any(), any(), any(), any()))
            .thenThrow(new TourNotFoundException(99L));

        mockMvc.perform(multipart("/api/v1/tours/{tourId}/task-assignments/new", 99L)
            .file(metadataPart))
            .andExpect(status().isNotFound());
    }

    @Test
    void createAndAssignTask_tourNotScheduled_shouldReturn400() throws Exception {
        CreateAndAssignTaskRequestDTO metadata = new CreateAndAssignTaskRequestDTO(
            "Task Title", "Task Description", AssignmentVisibility.VISIBLE, 25, validRequirements);

        MockMultipartFile metadataPart = new MockMultipartFile(
            "metadata", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(metadata));

        when(assignmentService.createAndAssignTask(eq(10L), any(), any(), any(), any()))
            .thenThrow(
                new CompetitionHierarchyValidationException("Cannot create task assignment. Tour has already started"));

        mockMvc.perform(multipart("/api/v1/tours/{tourId}/task-assignments/new", 10L)
            .file(metadataPart))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createAndAssignTask_blankTitle_shouldReturn400() throws Exception {
        CreateAndAssignTaskRequestDTO metadata = new CreateAndAssignTaskRequestDTO(
            "   ", "Task Description", AssignmentVisibility.VISIBLE, 25, validRequirements);

        MockMultipartFile metadataPart = new MockMultipartFile(
            "metadata", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(metadata));

        mockMvc.perform(multipart("/api/v1/tours/{tourId}/task-assignments/new", 10L)
            .file(metadataPart))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(assignmentService);
    }

    @Test
    void createAndAssignTask_nullMaxPoints_shouldReturn400() throws Exception {
        CreateAndAssignTaskRequestDTO metadata = new CreateAndAssignTaskRequestDTO(
            "Task Title", "Task Description", AssignmentVisibility.VISIBLE, null, validRequirements);

        MockMultipartFile metadataPart = new MockMultipartFile(
            "metadata", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(metadata));

        mockMvc.perform(multipart("/api/v1/tours/{tourId}/task-assignments/new", 10L)
            .file(metadataPart))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(assignmentService);
    }

    // GET /api/v1/task-assignments/{assignmentId} — getById

    @Test
    void getById_existingId_shouldReturn200() throws Exception {
        when(assignmentService.getTaskAssignmentById(1L)).thenReturn(mockDetailedResponse);

        mockMvc.perform(get("/api/v1/task-assignments/{assignmentId}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(mockDetailedResponse.id()))
            .andExpect(jsonPath("$.taskTitle").value(mockDetailedResponse.taskTitle()))
            .andExpect(jsonPath("$.taskDescription").value(mockDetailedResponse.taskDescription()))
            .andExpect(jsonPath("$.visibility").value(mockDetailedResponse.visibility().name()))
            .andExpect(jsonPath("$.files").isArray())
            .andExpect(jsonPath("$.files[0].originalFilename").value("problem.pdf"));
    }

    @Test
    void getById_nonExistingId_shouldReturn404() throws Exception {
        when(assignmentService.getTaskAssignmentById(99L)).thenThrow(new TaskAssignmentNotFoundException(99L));

        mockMvc.perform(get("/api/v1/task-assignments/{assignmentId}", 99L))
            .andExpect(status().isNotFound());
    }

    // GET /api/v1/tours/{tourId}/task-assignments — getByTour

    @Test
    void getByTour_shouldReturnPageResponseAnd200() throws Exception {
        when(assignmentService.getAssignmentsByTourId(any(Pageable.class), eq(10L)))
            .thenReturn(new PageImpl<>(List.of(mockAssignmentResponse)));

        mockMvc.perform(get("/api/v1/tours/{tourId}/task-assignments", 10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(mockAssignmentResponse.id()))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getByTour_tourNotFound_shouldReturn404() throws Exception {
        when(assignmentService.getAssignmentsByTourId(any(Pageable.class), eq(99L)))
            .thenThrow(new TourNotFoundException(99L));

        mockMvc.perform(get("/api/v1/tours/{tourId}/task-assignments", 99L))
            .andExpect(status().isNotFound());
    }

    // GET /api/v1/tasks/{taskId}/linked-tours — getLinkedTours

    @Test
    void getLinkedTours_shouldReturnListAnd200() throws Exception {
        LinkedToursResponseDTO mockTourResponse = LinkedToursResponseDTO.builder()
            .tourId(10L)
            .title("Tour 1")
            .description("Description")
            .location("Location")
            .executionStatus(ExecutionStatus.SCHEDULED)
            .build();

        when(assignmentService.getLinkedToursByTaskId(3L))
            .thenReturn(List.of(mockTourResponse));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/linked-tours", 3L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].tourId").value(10L))
            .andExpect(jsonPath("$[0].title").value("Tour 1"))
            .andExpect(jsonPath("$[0].description").value("Description"))
            .andExpect(jsonPath("$[0].location").value("Location"))
            .andExpect(jsonPath("$[0].executionStatus").value("SCHEDULED"));
    }

    @Test
    void getLinkedTours_taskNotFound_shouldReturn404() throws Exception {
        when(assignmentService.getLinkedToursByTaskId(99L))
            .thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/linked-tours", 99L))
            .andExpect(status().isNotFound());
    }

    @Test
    void getLinkedTours_tourNotFound_shouldReturn404() throws Exception {
        when(assignmentService.getLinkedToursByTaskId(3L))
            .thenThrow(new TourNotFoundException(10L));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/linked-tours", 3L))
            .andExpect(status().isNotFound());
    }

    // PATCH /api/v1/task-assignments/{assignmentId} — update

    @Test
    void update_validRequest_shouldReturn200() throws Exception {
        UpdateTaskAssignmentRequestDTO request = new UpdateTaskAssignmentRequestDTO(
            AssignmentVisibility.HIDDEN, 30, validRequirements, 0L);

        when(assignmentService.updateTaskAssignment(eq(1L), any(UpdateTaskAssignmentRequestDTO.class)))
            .thenReturn(mockDetailedResponse);

        mockMvc.perform(patch("/api/v1/task-assignments/{assignmentId}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(mockDetailedResponse.id()))
            .andExpect(jsonPath("$.taskDescription").value(mockDetailedResponse.taskDescription()))
            .andExpect(jsonPath("$.files").isArray());
    }

    @Test
    void update_notFound_shouldReturn404() throws Exception {
        UpdateTaskAssignmentRequestDTO request = new UpdateTaskAssignmentRequestDTO(
            AssignmentVisibility.HIDDEN, 30, validRequirements, 0L);

        when(assignmentService.updateTaskAssignment(eq(99L), any(UpdateTaskAssignmentRequestDTO.class)))
            .thenThrow(new TaskAssignmentNotFoundException(99L));

        mockMvc.perform(patch("/api/v1/task-assignments/{assignmentId}", 99L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void update_tourNotScheduled_shouldReturn400() throws Exception {
        UpdateTaskAssignmentRequestDTO request = new UpdateTaskAssignmentRequestDTO(
            AssignmentVisibility.HIDDEN, 30, validRequirements, 0L);

        when(assignmentService.updateTaskAssignment(eq(1L), any(UpdateTaskAssignmentRequestDTO.class)))
            .thenThrow(
                new CompetitionHierarchyValidationException("Cannot update task assignment. Tour has already started"));

        mockMvc.perform(patch("/api/v1/task-assignments/{assignmentId}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // DELETE /api/v1/task-assignments/{assignmentId} — delete

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(assignmentService).deleteTaskAssignment(1L);

        mockMvc.perform(delete("/api/v1/task-assignments/{assignmentId}", 1L))
            .andExpect(status().isNoContent());

        verify(assignmentService).deleteTaskAssignment(1L);
    }

    @Test
    void delete_notFound_shouldReturn404() throws Exception {
        doThrow(new TaskAssignmentNotFoundException(99L)).when(assignmentService).deleteTaskAssignment(99L);

        mockMvc.perform(delete("/api/v1/task-assignments/{assignmentId}", 99L))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_tourNotScheduled_shouldReturn400() throws Exception {
        doThrow(new CompetitionHierarchyValidationException("Cannot delete task assignment. Tour has already started"))
            .when(assignmentService).deleteTaskAssignment(1L);

        mockMvc.perform(delete("/api/v1/task-assignments/{assignmentId}", 1L))
            .andExpect(status().isBadRequest());
    }
}
