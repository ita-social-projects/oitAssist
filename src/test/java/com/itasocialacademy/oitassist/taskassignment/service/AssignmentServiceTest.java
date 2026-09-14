package com.itasocialacademy.oitassist.taskassignment.service;

import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskRequirementsDTO;
import java.util.Set;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.taskassignment.dto.response.LinkedToursResponseDTO;
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
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
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
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.DetailedTaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.StaleAssignmentVersionException;
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

    @Mock
    private FileManagerFacade fileManagerFacade;

    @Mock
    private SecurityFacade securityFacade;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private TaskAssignment taskAssignment;
    private TaskAssignmentResponseDTO assignmentResponse;
    private DetailedTaskAssignmentResponseDTO detailedResponse;
    private TourDetail tourDetail;
    private TaskBodyDetail taskBodyDetail;
    private TaskRequirements requirements;
    private TaskRequirementsRequestDTO reqDTO;
    private List<FileDetailsDTO> testFiles;

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
            .version(0L)
            .build();

        assignmentResponse = TaskAssignmentResponseDTO.builder()
            .id(1L)
            .taskBodyId(3L)
            .taskTitle("PowerPoint Різдвяна зірка")
            .tourId(10L)
            .maxPoints(25)
            .createdBy(100L)
            .build();

        testFiles = List.of(
            new FileDetailsDTO(1L, "problem.pdf", "application/pdf", 2048L, "PROBLEM",
                "/uploads/task/problem.pdf"));

        detailedResponse = new DetailedTaskAssignmentResponseDTO(
            1L, 3L, "PowerPoint Різдвяна зірка", "Створити у файлі-розв'язку",
            10L, AssignmentVisibility.VISIBLE, 25, requirements, testFiles, 100L, 0L);

        tourDetail = TourDetail.builder()
            .id(10L)
            .title("Tour 1")
            .executionStatus(ExecutionStatus.SCHEDULED)
            .build();

        taskBodyDetail = TaskBodyDetail.builder()
            .id(3L)
            .title("PowerPoint Різдвяна зірка")
            .description("Створити у файлі-розв'язку")
            .ownerIds(Set.of(100L))
            .build();
    }

    // ---- assignTask ----

    @Test
    void assignTask_validRequest_shouldSaveAndReturnDetailedResponse() {
        CreateTaskAssignmentRequestDTO request =
            new CreateTaskAssignmentRequestDTO(3L, AssignmentVisibility.VISIBLE, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(taskAssignmentRepository.existsByTaskBodyIdAndTourId(3L, 10L)).thenReturn(false);
        when(taskAssignmentMapper.toEntity(request)).thenReturn(taskAssignment);
        when(taskAssignmentRepository.save(taskAssignment)).thenReturn(taskAssignment);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(fileManagerFacade.getFilesByEntity(any(), eq(3L), any())).thenReturn(testFiles);
        when(taskAssignmentMapper.toDetailedResponse(taskAssignment, taskBodyDetail.title(),
            taskBodyDetail.description(), testFiles)).thenReturn(detailedResponse);

        DetailedTaskAssignmentResponseDTO result = assignmentService.assignTask(10L, request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(3L, result.taskBodyId());
        assertEquals(10L, result.tourId());
        assertEquals("Створити у файлі-розв'язку", result.taskDescription());
        assertEquals(testFiles, result.files());
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
    void assignTask_tourNotScheduled_shouldThrowCompetitionHierarchyValidationException() {
        CreateTaskAssignmentRequestDTO request =
            new CreateTaskAssignmentRequestDTO(3L, AssignmentVisibility.VISIBLE, 25, reqDTO);

        TourDetail activeTour = TourDetail.builder()
            .id(10L)
            .title("Tour 1")
            .executionStatus(ExecutionStatus.IN_PROGRESS)
            .build();

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(activeTour));

        assertThrows(CompetitionHierarchyValidationException.class, () -> assignmentService.assignTask(10L, request));
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
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(fileManagerFacade.getFilesByEntity(any(), eq(3L), any())).thenReturn(testFiles);
        when(taskAssignmentMapper.toDetailedResponse(any(), eq(taskBodyDetail.title()),
            eq(taskBodyDetail.description()), eq(testFiles))).thenReturn(detailedResponse);

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
    void getTaskAssignmentById_existingId_shouldReturnDetailedResponse() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(fileManagerFacade.getFilesByEntity(any(), eq(3L), any())).thenReturn(testFiles);
        when(taskAssignmentMapper.toDetailedResponse(taskAssignment, taskBodyDetail.title(),
            taskBodyDetail.description(), testFiles)).thenReturn(detailedResponse);

        DetailedTaskAssignmentResponseDTO result = assignmentService.getTaskAssignmentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("PowerPoint Різдвяна зірка", result.taskTitle());
        assertEquals("Створити у файлі-розв'язку", result.taskDescription());
        assertEquals(testFiles, result.files());
    }

    @Test
    void getTaskAssignmentById_nonExistingId_shouldThrowTaskAssignmentNotFoundException() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TaskAssignmentNotFoundException.class, () -> assignmentService.getTaskAssignmentById(1L));
    }

    @Test
    void getTaskAssignmentById_asParticipant_shouldResolveFilesWithoutSolutions() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.hasRole("ORG")).thenReturn(false);
        when(fileManagerFacade.getFilesByEntity(any(), eq(3L), any())).thenReturn(testFiles);
        when(taskAssignmentMapper.toDetailedResponse(any(), any(), any(), any())).thenReturn(detailedResponse);

        assignmentService.getTaskAssignmentById(1L);

        verify(securityFacade).hasRole("ADMIN");
        verify(securityFacade).hasRole("ORG");

        ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
        verify(fileManagerFacade).getFilesByEntity(any(), eq(3L), captor.capture());
        assertEquals(Set.of(FileRole.PROBLEM, FileRole.REFERENCE), captor.getValue());
    }

    // ---- updateTaskAssignment ----

    @Test
    void updateTaskAssignment_validRequest_shouldUpdateAndReturnDetailedResponse() {
        UpdateTaskAssignmentRequestDTO request =
            new UpdateTaskAssignmentRequestDTO(AssignmentVisibility.HIDDEN, 30, null, 0L);

        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(competitionFacade.findTourById(any())).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(taskAssignment);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(fileManagerFacade.getFilesByEntity(any(), eq(3L), any())).thenReturn(testFiles);
        when(taskAssignmentMapper.toDetailedResponse(any(), eq(taskBodyDetail.title()),
            eq(taskBodyDetail.description()), eq(testFiles))).thenReturn(detailedResponse);

        DetailedTaskAssignmentResponseDTO result = assignmentService.updateTaskAssignment(1L, request);

        assertNotNull(result);
        ArgumentCaptor<TaskAssignment> captor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentRepository).save(captor.capture());
        assertEquals(AssignmentVisibility.HIDDEN, captor.getValue().getVisibility());
        assertEquals(30, captor.getValue().getMaxPoints());
    }

    @Test
    void updateTaskAssignment_partialUpdate_shouldOnlyUpdateProvidedFields() {
        UpdateTaskAssignmentRequestDTO request =
            new UpdateTaskAssignmentRequestDTO(AssignmentVisibility.HIDDEN, null, null, 0L);
        Integer oldMaxPoints = taskAssignment.getMaxPoints();
        TaskRequirements oldRequirements = taskAssignment.getRequirements();

        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(competitionFacade.findTourById(any())).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(taskAssignment);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(fileManagerFacade.getFilesByEntity(any(), eq(3L), any())).thenReturn(testFiles);
        when(taskAssignmentMapper.toDetailedResponse(any(), any(), any(), any())).thenReturn(detailedResponse);

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
            new UpdateTaskAssignmentRequestDTO(AssignmentVisibility.HIDDEN, 30, null, 0L);

        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TaskAssignmentNotFoundException.class, () -> assignmentService.updateTaskAssignment(1L, request));
    }

    @Test
    void updateTaskAssignment_tourNotScheduled_shouldThrowCompetitionHierarchyValidationException() {
        UpdateTaskAssignmentRequestDTO request =
            new UpdateTaskAssignmentRequestDTO(AssignmentVisibility.HIDDEN, 30, null, 0L);

        TourDetail activeTour = TourDetail.builder()
            .id(10L)
            .title("Tour 1")
            .executionStatus(ExecutionStatus.IN_PROGRESS)
            .build();

        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(activeTour));

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> assignmentService.updateTaskAssignment(1L, request));
    }

    // ---- deleteTaskAssignment ----

    @Test
    void deleteTaskAssignment_existingId_shouldDelete() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(competitionFacade.findTourById(any())).thenReturn(Optional.of(tourDetail));

        assignmentService.deleteTaskAssignment(1L);

        verify(taskAssignmentRepository).delete(taskAssignment);
    }

    @Test
    void deleteTaskAssignment_nonExistingId_shouldThrowTaskAssignmentNotFoundException() {
        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TaskAssignmentNotFoundException.class, () -> assignmentService.deleteTaskAssignment(1L));
        verify(taskAssignmentRepository, never()).delete(any());
    }

    @Test
    void deleteTaskAssignment_tourNotScheduled_shouldThrowCompetitionHierarchyValidationException() {
        TourDetail activeTour = TourDetail.builder()
            .id(10L)
            .title("Tour 1")
            .executionStatus(ExecutionStatus.IN_PROGRESS)
            .build();

        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(activeTour));

        assertThrows(CompetitionHierarchyValidationException.class, () -> assignmentService.deleteTaskAssignment(1L));
        verify(taskAssignmentRepository, never()).delete(any());
    }

    // ---- createAndAssignTask ----

    @Test
    void createAndAssignTask_validRequest_shouldCreateTaskAndAssignment() {
        CreateAndAssignTaskRequestDTO request =
            new CreateAndAssignTaskRequestDTO("PowerPoint Різдвяна зірка", "Створити у файлі-розв'язку",
                AssignmentVisibility.VISIBLE, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.createTask(request.title(), request.description(), null, null, null)).thenReturn(
            taskBodyDetail);
        when(taskAssignmentMapper.toRequirements(reqDTO)).thenReturn(requirements);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(taskAssignment);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(fileManagerFacade.getFilesByEntity(any(), eq(3L), any())).thenReturn(testFiles);
        when(taskAssignmentMapper.toDetailedResponse(taskAssignment, request.title(), request.description(), testFiles))
            .thenReturn(detailedResponse);

        DetailedTaskAssignmentResponseDTO result =
            assignmentService.createAndAssignTask(10L, request, null, null, null);

        assertNotNull(result);
        verify(taskBodyFacade).createTask("PowerPoint Різдвяна зірка", "Створити у файлі-розв'язку", null, null, null);

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
                AssignmentVisibility.VISIBLE, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.empty());

        assertThrows(TourNotFoundException.class,
            () -> assignmentService.createAndAssignTask(10L, request, null, null, null));
        verify(taskBodyFacade, never()).createTask(any(), any(), any(), any(), any());
        verify(taskAssignmentRepository, never()).save(any());
    }

    @Test
    void createAndAssignTask_tourNotScheduled_shouldThrowCompetitionHierarchyValidationException() {
        CreateAndAssignTaskRequestDTO request =
            new CreateAndAssignTaskRequestDTO("PowerPoint Різдвяна зірка", "Створити у файлі-розв'язку",
                AssignmentVisibility.VISIBLE, 25, reqDTO);

        TourDetail activeTour = TourDetail.builder()
            .id(10L)
            .title("Tour 1")
            .executionStatus(ExecutionStatus.IN_PROGRESS)
            .build();

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(activeTour));

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> assignmentService.createAndAssignTask(10L, request, null, null, null));
        verify(taskBodyFacade, never()).createTask(any(), any(), any(), any(), any());
        verify(taskAssignmentRepository, never()).save(any());
    }

    @Test
    void createAndAssignTask_nullVisibility_shouldDefaultToHidden() {
        CreateAndAssignTaskRequestDTO request =
            new CreateAndAssignTaskRequestDTO("PowerPoint Різдвяна зірка", "Створити у файлі-розв'язку",
                null, 25, reqDTO);

        when(competitionFacade.findTourById(10L)).thenReturn(Optional.of(tourDetail));
        when(taskBodyFacade.createTask(request.title(), request.description(), null, null, null)).thenReturn(
            taskBodyDetail);
        when(taskAssignmentMapper.toRequirements(reqDTO)).thenReturn(requirements);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(taskAssignment);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(fileManagerFacade.getFilesByEntity(any(), eq(3L), any())).thenReturn(testFiles);
        when(taskAssignmentMapper.toDetailedResponse(taskAssignment, taskBodyDetail.title(),
            taskBodyDetail.description(), testFiles)).thenReturn(detailedResponse);

        assignmentService.createAndAssignTask(10L, request, null, null, null);

        ArgumentCaptor<TaskAssignment> captor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentRepository).save(captor.capture());
        assertEquals(AssignmentVisibility.HIDDEN, captor.getValue().getVisibility());
    }

    // ---- getTaskAssignmentDetailById ----

    @Test
    void getTaskAssignmentDetailById_existingId_shouldReturnDetail() {
        TaskRequirementsDTO requirementsDto = new TaskRequirementsDTO(List.of(
            new TaskRequirementsDTO.RequiredFileDTO("PowerPoint_РіздвянаЗірка",
                List.of(".pptx"), 50)));
        TaskAssignmentDetailDTO detailDTO = new TaskAssignmentDetailDTO(
            1L, 3L, 10L, AssignmentVisibility.VISIBLE, 25, requirementsDto);

        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(taskAssignmentMapper.toDetails(taskAssignment)).thenReturn(detailDTO);

        Optional<TaskAssignmentDetailDTO> result = assignmentService.getTaskAssignmentDetailById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().id());
        assertEquals(3L, result.get().taskBodyId());
        assertEquals(10L, result.get().tourId());
    }

    @Test
    void getTaskAssignmentDetailById_nonExistingId_shouldReturnEmpty() {
        when(taskAssignmentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<TaskAssignmentDetailDTO> result = assignmentService.getTaskAssignmentDetailById(99L);

        assertTrue(result.isEmpty());
        verify(taskAssignmentMapper, never()).toDetails(any());
    }

    // ---- existsByTaskBodyId ----

    @Test
    void existsByTaskBodyId_whenExists_shouldReturnTrue() {
        when(taskAssignmentRepository.existsByTaskBodyId(3L)).thenReturn(true);

        assertTrue(assignmentService.existsByTaskBodyId(3L));
    }

    @Test
    void existsByTaskBodyId_whenNotExists_shouldReturnFalse() {
        when(taskAssignmentRepository.existsByTaskBodyId(99L)).thenReturn(false);

        assertFalse(assignmentService.existsByTaskBodyId(99L));
    }

    // ---- getLinkedToursByTaskId ----

    @Test
    void getLinkedToursByTaskId_whenTaskExistsAndUserAuthorized_shouldReturnLinkedTours() {
        LinkedToursResponseDTO mockTourResponse = LinkedToursResponseDTO.builder()
            .tourId(10L)
            .title("Tour 1")
            .description("Description")
            .location("Location")
            .executionStatus(ExecutionStatus.SCHEDULED)
            .build();

        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskAssignmentRepository.findTourIdsByTaskBodyId(3L)).thenReturn(List.of(10L));
        when(competitionFacade.findToursByIds(List.of(10L))).thenReturn(List.of(tourDetail));
        when(taskAssignmentMapper.toLinkedToursResponse(tourDetail)).thenReturn(mockTourResponse);

        List<LinkedToursResponseDTO> result = assignmentService.getLinkedToursByTaskId(3L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.getFirst().tourId());
        assertEquals("Tour 1", result.getFirst().title());
        assertEquals("Description", result.getFirst().description());
        assertEquals("Location", result.getFirst().location());
        assertEquals(ExecutionStatus.SCHEDULED, result.getFirst().executionStatus());
    }

    @Test
    void getLinkedToursByTaskId_whenTaskDoesNotExist_shouldThrowTaskNotFoundException() {
        when(taskBodyFacade.findTaskBodyById(99L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> assignmentService.getLinkedToursByTaskId(99L));
        verify(taskAssignmentRepository, never()).findTourIdsByTaskBodyId(any());
    }

    @Test
    void getLinkedToursByTaskId_whenUserNotAuthorized_shouldThrowAuthorizationException() {
        when(taskBodyFacade.findTaskBodyById(3L)).thenReturn(Optional.of(taskBodyDetail));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.hasRole("ORG")).thenReturn(false);

        assertThrows(com.itasocialacademy.oitassist.core.exceptions.AuthorizationException.class,
            () -> assignmentService.getLinkedToursByTaskId(3L));
        verify(taskAssignmentRepository, never()).findTourIdsByTaskBodyId(any());
    }

    // ---- version conflict (optimistic locking) ----

    @Test
    void updateTaskAssignment_staleVersion_shouldThrowStaleAssignmentVersionException() {
        UpdateTaskAssignmentRequestDTO request =
            new UpdateTaskAssignmentRequestDTO(AssignmentVisibility.HIDDEN, 30, null, 999L);

        when(taskAssignmentRepository.findById(1L)).thenReturn(Optional.of(taskAssignment));
        when(competitionFacade.findTourById(any())).thenReturn(Optional.of(tourDetail));

        assertThrows(StaleAssignmentVersionException.class,
            () -> assignmentService.updateTaskAssignment(1L, request));

        verify(taskAssignmentRepository, never()).save(any());
    }
}
