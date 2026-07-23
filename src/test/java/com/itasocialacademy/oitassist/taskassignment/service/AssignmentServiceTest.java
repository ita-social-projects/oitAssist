package com.itasocialacademy.oitassist.taskassignment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.task.api.TaskBodyFacade;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskAssignment;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskRequirements;
import com.itasocialacademy.oitassist.taskassignment.dao.repository.TaskAssignmentRepository;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.UpdateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.TaskRequirementsRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateAndAssignTaskRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAlreadyAssignedException;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import com.itasocialacademy.oitassist.taskassignment.mapper.TaskAssignmentMapper;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private TaskAssignmentMapper taskAssignmentMapper;

    @Mock
    private CompetitionFacade competitionFacade;

    @Mock
    private TaskBodyFacade taskBodyFacade;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private TaskAssignment taskAssignment;
    private TaskAssignmentResponseDTO assignmentResponse;
    private TourDetail tourDetail;
    private TaskBodyDetail taskBodyDetail;
    private TaskRequirements requirements;
    private TaskRequirementsRequestDTO reqDTO;

    @BeforeEach
    void setUp() {
        requirements = new TaskRequirements(List.of(
            new TaskRequirements.RequiredFile("Файл розв'язку", "PowerPoint_РіздвянаЗірка",
                List.of(".pptx"), 50)));

        reqDTO = new TaskRequirementsRequestDTO(List.of(
            new TaskRequirementsRequestDTO.RequiredFileRequest("Файл розв'язку", "PowerPoint_РіздвянаЗірка",
                List.of(".pptx"), 50)));

        taskAssignment = TaskAssignment.builder()
            .id(1L)
            .taskBodyId(3L)
            .tourId(10L)
            .visibility(AssignmentVisibility.VISIBLE)
            .maxPoints(25)
            .requirements(requirements)
            .createdBy(100L)
            .build();

        assignmentResponse = TaskAssignmentResponseDTO.builder()
            .id(1L)
            .taskBodyId(3L)
            .taskTitle("PowerPoint Різдвяна зірка")
            .tourId(10L)
            .visibility(AssignmentVisibility.VISIBLE)
            .maxPoints(25)
            .requirements(requirements)
            .createdBy(100L)
            .build();

        tourDetail = TourDetail.builder()
            .id(10L)
            .title("Tour 1")
            .build();

        taskBodyDetail = TaskBodyDetail.builder()
            .id(3L)
            .title("PowerPoint Різдвяна зірка")
            .description("Створити у файлі-розв'язку")
            .ownerId(100L)
            .build();
    }

    // ---- assignTask ----

    @Test
    void assignTask_validRequest_shouldSaveAndReturnResponse() {
        CreateTaskAssignmentRequestDTO request =
            new CreateTaskAssignmentRequestDTO(3L, AssignmentVisibility.VISIBLE, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(taskAssignmentRepository.existsByTaskBodyIdAndTourId(3L, 10L)).thenReturn(false);
        when(taskAssignmentMapper.toEntity(request)).thenReturn(taskAssignment);
        when(taskAssignmentRepository.save(taskAssignment)).thenReturn(taskAssignment);
        when(taskAssignmentMapper.toResponse(taskAssignment, taskBodyDetail.title())).thenReturn(assignmentResponse);

        TaskAssignmentResponseDTO result = assignmentService.assignTask(10L, request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(3L, result.taskBodyId());
        assertEquals(10L, result.tourId());
        verify(taskAssignmentRepository).save(taskAssignment);
    }

    @Test
    void assignTask_tourNotFound_shouldThrowTourNotFoundException() {
        CreateTaskAssignmentRequestDTO request =
            new CreateTaskAssignmentRequestDTO(3L, AssignmentVisibility.VISIBLE, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.empty());

        assertThrows(TourNotFoundException.class, () -> assignmentService.assignTask(10L, request));
        verify(taskAssignmentRepository, never()).save(any());
    }

    @Test
    void assignTask_taskNotFound_shouldThrowTaskNotFoundException() {
        CreateTaskAssignmentRequestDTO request =
            new CreateTaskAssignmentRequestDTO(3L, AssignmentVisibility.VISIBLE, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> assignmentService.assignTask(10L, request));
    }

    @Test
    void assignTask_duplicateAssignment_shouldThrowTaskAlreadyAssignedException() {
        CreateTaskAssignmentRequestDTO request =
            new CreateTaskAssignmentRequestDTO(3L, AssignmentVisibility.VISIBLE, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(taskAssignmentRepository.existsByTaskBodyIdAndTourId(3L, 10L)).thenReturn(true);

        assertThrows(TaskAlreadyAssignedException.class, () -> assignmentService.assignTask(10L, request));
        verify(taskAssignmentRepository, never()).save(any());
    }

    @Test
    void assignTask_nullVisibility_shouldDefaultToHidden() {
        CreateTaskAssignmentRequestDTO request = new CreateTaskAssignmentRequestDTO(3L, null, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(taskAssignmentRepository.existsByTaskBodyIdAndTourId(3L, 10L)).thenReturn(false);
        when(taskAssignmentMapper.toEntity(request)).thenReturn(taskAssignment);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(taskAssignment);
        when(taskAssignmentMapper.toResponse(taskAssignment, taskBodyDetail.title())).thenReturn(assignmentResponse);

        assignmentService.assignTask(10L, request);

        ArgumentCaptor<TaskAssignment> captor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentRepository).save(captor.capture());
        assertEquals(AssignmentVisibility.HIDDEN, captor.getValue().getVisibility());
    }

    // ---- getAssignmentsByTourId ----

    @Test
    void getAssignmentsByTourId_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskAssignment> page = new PageImpl<>(List.of(taskAssignment));

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskAssignmentRepository.findAllByTourId(10L, pageable)).thenReturn(page);
        when(taskBodyFacade.getTaskTitlesByIds(List.of(3L))).thenReturn(Map.of(3L, "PowerPoint Різдвяна зірка"));
        when(taskAssignmentMapper.toResponse(taskAssignment, "PowerPoint Різдвяна зірка")).thenReturn(
            assignmentResponse);

        Page<TaskAssignmentResponseDTO> result = assignmentService.getAssignmentsByTourId(pageable, 10L);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAssignmentsByTourId_emptyPage_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskAssignmentRepository.findAllByTourId(10L, pageable)).thenReturn(Page.empty(pageable));

        Page<TaskAssignmentResponseDTO> result = assignmentService.getAssignmentsByTourId(pageable, 10L);

        assertTrue(result.isEmpty());
        verify(taskBodyFacade, never()).getTaskTitlesByIds(any());
    }

    @Test
    void getAssignmentsByTourId_tourNotFound_shouldThrowTourNotFoundException() {
        Pageable pageable = PageRequest.of(0, 10);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.empty());

        assertThrows(TourNotFoundException.class, () -> assignmentService.getAssignmentsByTourId(pageable, 10L));
    }

    // ---- getTaskAssignmentById ----

    @Test
    void getTaskAssignmentById_existingId_shouldReturnResponse() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(taskAssignmentMapper.toResponse(taskAssignment, taskBodyDetail.title())).thenReturn(assignmentResponse);

        TaskAssignmentResponseDTO result = assignmentService.getTaskAssignmentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void getTaskAssignmentById_nonExistingId_shouldThrowTaskAssignmentNotFoundException() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TaskAssignmentNotFoundException.class, () -> assignmentService.getTaskAssignmentById(1L));
    }

    // ---- updateTaskAssignment ----

    @Test
    void updateTaskAssignment_validRequest_shouldUpdateAndReturnResponse() {
        UpdateTaskAssignmentRequestDTO request =
            new UpdateTaskAssignmentRequestDTO(AssignmentVisibility.HIDDEN, 30, null);

        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(taskAssignment);
        when(taskAssignmentMapper.toResponse(taskAssignment, taskBodyDetail.title())).thenReturn(assignmentResponse);

        TaskAssignmentResponseDTO result = assignmentService.updateTaskAssignment(1L, request);

        assertNotNull(result);
        ArgumentCaptor<TaskAssignment> captor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentRepository).save(captor.capture());
        assertEquals(AssignmentVisibility.HIDDEN, captor.getValue().getVisibility());
        assertEquals(30, captor.getValue().getMaxPoints());
    }

    @Test
    void updateTaskAssignment_partialUpdate_shouldOnlyUpdateProvidedFields() {
        UpdateTaskAssignmentRequestDTO request =
            new UpdateTaskAssignmentRequestDTO(AssignmentVisibility.HIDDEN, null, null);
        Integer oldMaxPoints = taskAssignment.getMaxPoints();
        TaskRequirements oldRequirements = taskAssignment.getRequirements();

        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(taskAssignment);
        when(taskAssignmentMapper.toResponse(taskAssignment, taskBodyDetail.title())).thenReturn(assignmentResponse);

        assignmentService.updateTaskAssignment(1L, request);

        ArgumentCaptor<TaskAssignment> captor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentRepository).save(captor.capture());
        assertEquals(AssignmentVisibility.HIDDEN, captor.getValue().getVisibility());
        assertEquals(oldMaxPoints, captor.getValue().getMaxPoints());
        assertEquals(oldRequirements, captor.getValue().getRequirements());
    }

    @Test
    void updateTaskAssignment_notFound_shouldThrowTaskAssignmentNotFoundException() {
        UpdateTaskAssignmentRequestDTO request =
            new UpdateTaskAssignmentRequestDTO(AssignmentVisibility.HIDDEN, 30, null);

        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TaskAssignmentNotFoundException.class, () -> assignmentService.updateTaskAssignment(1L, request));
    }

    // ---- deleteTaskAssignment ----

    @Test
    void deleteTaskAssignment_existingId_shouldDelete() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));

        assignmentService.deleteTaskAssignment(1L);

        verify(taskAssignmentRepository).delete(taskAssignment);
    }

    @Test
    void deleteTaskAssignment_nonExistingId_shouldThrowTaskAssignmentNotFoundException() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TaskAssignmentNotFoundException.class, () -> assignmentService.deleteTaskAssignment(1L));
        verify(taskAssignmentRepository, never()).delete(any());
    }

    // ---- createAndAssignTask ----

    @Test
    void createAndAssignTask_validRequest_shouldCreateTaskAndAssignment() {
        CreateAndAssignTaskRequestDTO request =
            new CreateAndAssignTaskRequestDTO("PowerPoint Різдвяна зірка", "Створити у файлі-розв'язку",
                List.of(1L, 2L), AssignmentVisibility.VISIBLE, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.createTask(request.title(), request.description(), request.fileIds())).thenReturn(
            taskBodyDetail);
        when(taskAssignmentMapper.toRequirements(reqDTO)).thenReturn(requirements);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(taskAssignment);
        when(taskAssignmentMapper.toResponse(taskAssignment, taskBodyDetail.title())).thenReturn(assignmentResponse);

        TaskAssignmentResponseDTO result = assignmentService.createAndAssignTask(10L, request);

        assertNotNull(result);
        verify(taskBodyFacade).createTask("PowerPoint Різдвяна зірка", "Створити у файлі-розв'язку", List.of(1L, 2L));

        ArgumentCaptor<TaskAssignment> captor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentRepository).save(captor.capture());

        TaskAssignment saved = captor.getValue();
        assertEquals(3L, saved.getTaskBodyId());
        assertEquals(10L, saved.getTourId());
        assertEquals(AssignmentVisibility.VISIBLE, saved.getVisibility());
        assertEquals(25, saved.getMaxPoints());
    }

    @Test
    void createAndAssignTask_tourNotFound_shouldThrowTourNotFoundException() {
        CreateAndAssignTaskRequestDTO request =
            new CreateAndAssignTaskRequestDTO("PowerPoint Різдвяна зірка", "Створити у файлі-розв'язку",
                List.of(1L, 2L), AssignmentVisibility.VISIBLE, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.empty());

        assertThrows(TourNotFoundException.class, () -> assignmentService.createAndAssignTask(10L, request));
        verify(taskBodyFacade, never()).createTask(any(), any(), any());
        verify(taskAssignmentRepository, never()).save(any());
    }

    @Test
    void createAndAssignTask_nullVisibility_shouldDefaultToHidden() {
        CreateAndAssignTaskRequestDTO request =
            new CreateAndAssignTaskRequestDTO("PowerPoint Різдвяна зірка", "Створити у файлі-розв'язку",
                List.of(1L, 2L), null, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.createTask(request.title(), request.description(), request.fileIds())).thenReturn(
            taskBodyDetail);
        when(taskAssignmentMapper.toRequirements(reqDTO)).thenReturn(requirements);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(taskAssignment);
        when(taskAssignmentMapper.toResponse(taskAssignment, taskBodyDetail.title())).thenReturn(assignmentResponse);

        assignmentService.createAndAssignTask(10L, request);

        ArgumentCaptor<TaskAssignment> captor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentRepository).save(captor.capture());
        assertEquals(AssignmentVisibility.HIDDEN, captor.getValue().getVisibility());
    }
}
