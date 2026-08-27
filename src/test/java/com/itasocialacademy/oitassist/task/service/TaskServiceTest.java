package com.itasocialacademy.oitassist.task.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.api.events.FilesAttachRequestedEvent;
import com.itasocialacademy.oitassist.filemanager.api.events.FilesDetachRequestedEvent;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.task.dao.model.TaskBody;
import com.itasocialacademy.oitassist.task.dao.model.TaskOwner;
import com.itasocialacademy.oitassist.task.dao.model.id.TaskOwnerId;
import com.itasocialacademy.oitassist.task.dao.repository.TaskBodyRepository;
import com.itasocialacademy.oitassist.task.dto.request.AddOwnerRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.RemoveOwnerRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.UpdateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import com.itasocialacademy.oitassist.task.exceptions.StaleTaskVersionException;
import com.itasocialacademy.oitassist.task.exceptions.TaskAccessRestrictedException;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.task.mapper.TaskBodyMapper;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskBodyRepository taskBodyRepository;
    @Mock
    private TaskBodyMapper taskBodyMapper;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private SecurityFacade securityFacade;
    @Mock
    private UserFacade userFacade;
    @Mock
    private FileManagerFacade fileManagerFacade;

    @InjectMocks
    private TaskServiceImpl taskService;

    private TaskBody taskBody;
    private TaskResponseDTO taskResponse;
    private List<FileDetailsDTO> testFiles;

    @BeforeEach
    void setUp() {
        taskBody = TaskBody.builder()
            .id(1L)
            .title("Test Task")
            .description("Test Description")
            .createdBy(100L)
            .version(0L)
            .build();

        taskBody.setOwners(new HashSet<>(Set.of(
            TaskOwner.builder()
                .id(new TaskOwnerId(1L, 100L))
                .task(taskBody)
                .build())));

        testFiles = List.of(
            new FileDetailsDTO(1L, "problem.pdf", "application/pdf", 2048L, "PROBLEM",
                "/uploads/task/problem.pdf"));

        taskResponse = TaskResponseDTO.builder()
            .id(1L)
            .title("Test Task")
            .description("Test Description")
            .createdBy(100L)
            .createdByEmail("creator@mail.com")
            .ownerIds(new HashSet<>(Set.of(100L)))
            .files(testFiles)
            .build();

        lenient().when(userFacade.findProfileById(100L))
            .thenReturn(Optional.of(new UserProfileDetails(100L, "Creator", "creator@mail.com")));

        lenient().when(userFacade.findByIds(List.of(100L)))
            .thenReturn(List.of(new UserAuthDetails(100L, "creator@mail.com", "pass", Role.ADMIN)));
    }

    // ---- createTask ----

    @Test
    void createTask_validRequest_shouldSaveAndReturnResponse() {
        CreateTaskRequestDTO request = new CreateTaskRequestDTO(
            "Test Task", "Test Description", List.of(51L, 52L));
        TaskBody taskWithoutOwners = TaskBody.builder()
            .id(1L)
            .title("Test Task")
            .description("Test Description")
            .createdBy(100L)
            .owners(new HashSet<>())
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));
        when(taskBodyMapper.toEntity(request)).thenReturn(taskWithoutOwners);
        when(taskBodyRepository.save(any(TaskBody.class))).thenReturn(taskWithoutOwners);
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskWithoutOwners, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        TaskResponseDTO result = taskService.createTask(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Test Task", result.title());

        ArgumentCaptor<TaskBody> captor = ArgumentCaptor.forClass(TaskBody.class);
        verify(taskBodyRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getOwners().size());

        TaskOwner owner = captor.getValue().getOwners().iterator().next();
        assertEquals(100L, owner.getId().getOwnerId());
    }

    @Test
    void createTask_validRequest_shouldPublishAttachEvent() {
        CreateTaskRequestDTO request = new CreateTaskRequestDTO(
            "Test Task", "Test Description", List.of(51L, 52L));

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));
        when(taskBodyMapper.toEntity(request)).thenReturn(taskBody);
        when(taskBodyRepository.save(any(TaskBody.class))).thenReturn(taskBody);
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        taskService.createTask(request);

        ArgumentCaptor<FilesAttachRequestedEvent> eventCaptor =
            ArgumentCaptor.forClass(FilesAttachRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        assertNotNull(eventCaptor.getValue());
    }

    @Test
    void createTask_withNullFileIds_shouldNotPublishAttachEvent() {
        CreateTaskRequestDTO request = new CreateTaskRequestDTO(
            "Test Task", "Test Description", null);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));
        when(taskBodyMapper.toEntity(request)).thenReturn(taskBody);
        when(taskBodyRepository.save(any(TaskBody.class))).thenReturn(taskBody);
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        taskService.createTask(request);

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createTask_notLoggedIn_shouldThrowAuthorizationException() {
        CreateTaskRequestDTO request = new CreateTaskRequestDTO(
            "Test Task", "Test Description", List.of(1L));

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthorizationException.class, () -> taskService.createTask(request));

        verify(taskBodyRepository, never()).save(any());
    }

    @Test
    void createTask_shouldNotSetAuditFieldsManually() {
        CreateTaskRequestDTO request = new CreateTaskRequestDTO(
            "Test Task", "Test Description", List.of(1L));

        TaskBody freshEntity = new TaskBody();
        freshEntity.setId(1L);
        freshEntity.setTitle("Test Task");
        freshEntity.setCreatedBy(100L);
        freshEntity.setOwners(new HashSet<>(Set.of(
            TaskOwner.builder()
                .id(new TaskOwnerId(1L, 100L))
                .build())));

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));
        when(taskBodyMapper.toEntity(request)).thenReturn(freshEntity);
        when(taskBodyRepository.save(any(TaskBody.class))).thenReturn(freshEntity);
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(freshEntity, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        taskService.createTask(request);

        ArgumentCaptor<TaskBody> captor = ArgumentCaptor.forClass(TaskBody.class);
        verify(taskBodyRepository).save(captor.capture());
        TaskBody saved = captor.getValue();

        assertNull(saved.getUpdatedBy());
        assertNull(saved.getCreatedAt());
        assertNull(saved.getUpdatedAt());
    }

    // ---- getTaskById ----

    @Test
    void getTaskById_existingId_shouldReturnResponse() {
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        TaskResponseDTO result = taskService.getTaskById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(taskBodyRepository).findById(1L);
    }

    @Test
    void getTaskById_asAdmin_shouldReturnResponse() {
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(50L));
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        TaskResponseDTO result = taskService.getTaskById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(taskBodyRepository).findById(1L);
    }

    @Test
    void getTaskById_notOwnerNotAdmin_shouldThrowTaskAccessRestrictedException() {
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(50L));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        assertThrows(TaskAccessRestrictedException.class, () -> taskService.getTaskById(1L));
    }

    @Test
    void getTaskById_nonExistingId_shouldThrowTaskNotFoundException() {
        when(taskBodyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(99L));
    }

    // ---- getAllTasks ----

    @Test
    void getAllTasks_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<TaskBody> page = new PageImpl<>(List.of(taskBody), pageable, 1);
        String search = " scratch   ";
        String normalizedSearch = "scratch";

        when(taskBodyRepository.findAllByTitleLike(normalizedSearch, pageable)).thenReturn(page);
        when(fileManagerFacade.getFilesByEntities(any(), eq(List.of(1L)), any())).thenReturn(Map.of(1L, testFiles));
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        Page<TaskResponseDTO> result = taskService.getAllTasks(pageable, search);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(taskResponse, result.getContent().getFirst());

        verify(taskBodyRepository).findAllByTitleLike(normalizedSearch, pageable);
        verify(taskBodyMapper).toResponse(taskBody, testFiles, "creator@mail.com");
    }

    @Test
    void getAllTasks_whenEmpty_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<TaskBody> emptyPage = Page.empty(pageable);

        when(taskBodyRepository.findAllByTitleLike("", pageable)).thenReturn(emptyPage);

        Page<TaskResponseDTO> result = taskService.getAllTasks(pageable, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskBodyMapper, never()).toResponse(any(), any(), anyString());
    }

    // ---- getAllMyTasks ----

    @Test
    void getAllMyTasks_shouldReturnCurrentUserTasks() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<TaskBody> expectedRepositoryPage = new PageImpl<>(List.of(taskBody), pageable, 1);
        String search = " scratch   ";
        String normalizedSearch = "scratch";

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));
        when(taskBodyRepository.findAllByOwnerId(100L, normalizedSearch, pageable)).thenReturn(expectedRepositoryPage);
        when(fileManagerFacade.getFilesByEntities(any(), eq(List.of(1L)), any())).thenReturn(Map.of(1L, testFiles));
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        Page<TaskResponseDTO> result = taskService.getAllMyTasks(pageable, search);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(securityFacade).getCurrentUserId();
        verify(taskBodyRepository).findAllByOwnerId(100L, normalizedSearch, pageable);
        verify(taskBodyMapper).toResponse(taskBody, testFiles, "creator@mail.com");
    }

    @Test
    void getAllMyTasks_notLoggedIn_shouldThrowAuthorizationException() {
        Pageable pageable = PageRequest.of(0, 15);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthorizationException.class, () -> taskService.getAllMyTasks(pageable, null));

        verify(taskBodyRepository, never()).findAllByOwnerId(any(), any(), any());
    }

    // ---- updateTask ----

    @Test
    void updateTask_asOwner_shouldUpdateFieldsAndPublishBothEvents() {
        UpdateTaskRequestDTO request = new UpdateTaskRequestDTO(
            "Updated Title", "Updated Description", List.of(62L), List.of(51L), 0L);

        TaskResponseDTO updatedResponse = TaskResponseDTO.builder()
            .id(1L)
            .title("Updated Title")
            .description("Updated Description")
            .createdBy(100L)
            .createdByEmail("creator@mail.com")
            .ownerIds(new HashSet<>(Set.of(100L)))
            .build();

        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(taskBodyRepository.saveAndFlush(any(TaskBody.class))).thenReturn(taskBody);
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(updatedResponse);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));

        TaskResponseDTO result = taskService.updateTask(1L, request);

        assertNotNull(result);
        assertEquals("Updated Title", result.title());
        assertEquals("Updated Description", result.description());
        assertEquals("creator@mail.com", result.createdByEmail());

        ArgumentCaptor<TaskBody> captor = ArgumentCaptor.forClass(TaskBody.class);
        verify(taskBodyRepository).saveAndFlush(captor.capture());
        assertEquals("Updated Title", captor.getValue().getTitle());
        assertEquals("Updated Description", captor.getValue().getDescription());

        // Verify both attach and detach events are published
        verify(applicationEventPublisher).publishEvent(any(FilesAttachRequestedEvent.class));
        verify(applicationEventPublisher).publishEvent(any(FilesDetachRequestedEvent.class));
    }

    @Test
    void updateTask_shouldPublishAttachEvent() {
        UpdateTaskRequestDTO request = new UpdateTaskRequestDTO(
            "Updated Title", "Updated Description", List.of(62L), null, 0L);

        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.saveAndFlush(any(TaskBody.class))).thenReturn(taskBody);
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));

        taskService.updateTask(1L, request);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertInstanceOf(FilesAttachRequestedEvent.class, eventCaptor.getValue());
    }

    @Test
    void updateTask_shouldPublishDetachEvent() {
        UpdateTaskRequestDTO request = new UpdateTaskRequestDTO(
            "Updated Title", "Updated Description", null, List.of(51L), 0L);

        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.saveAndFlush(any(TaskBody.class))).thenReturn(taskBody);
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));

        taskService.updateTask(1L, request);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertInstanceOf(FilesDetachRequestedEvent.class, eventCaptor.getValue());
    }

    @Test
    void updateTask_nonExistingTask_shouldThrowTaskNotFoundException() {
        UpdateTaskRequestDTO request = new UpdateTaskRequestDTO(
            "Title", "Description", null, null, 0L);

        when(taskBodyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.updateTask(99L, request));

        verify(taskBodyRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTask_notOwnerNotAdmin_shouldThrowTaskAccessRestrictedException() {
        UpdateTaskRequestDTO request = new UpdateTaskRequestDTO(
            "Title", "Description", null, null, 0L);

        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(50L));

        assertThrows(TaskAccessRestrictedException.class, () -> taskService.updateTask(1L, request));

        verify(taskBodyRepository, never()).saveAndFlush(any());
    }

    // ---- addTaskOwner ----

    @Test
    void addTaskOwner_asAdmin_toOrgUser_shouldSucceed() {
        AddOwnerRequestDTO request = new AddOwnerRequestDTO("newowner@mail.com", 0L);
        UserAuthDetails newOwner =
            new UserAuthDetails(200L, "newowner@mail.com", "12345678", Role.ORG);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(userFacade.findByEmail("newowner@mail.com")).thenReturn(Optional.of(newOwner));
        when(taskBodyRepository.saveAndFlush(any(TaskBody.class))).thenReturn(taskBody);
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(any(TaskBody.class), any(), eq("creator@mail.com"))).thenReturn(taskResponse);

        TaskResponseDTO result = taskService.addTaskOwner(1L, request);

        assertNotNull(result);

        assertTrue(taskBody.getOwners().stream()
            .anyMatch(owner -> owner.getId().getOwnerId().equals(200L)));

        verify(taskBodyMapper).toResponse(taskBody, testFiles, "creator@mail.com");
    }

    @Test
    void addTaskOwner_notAdmin_shouldThrowTaskAccessRestrictedException() {
        AddOwnerRequestDTO request = new AddOwnerRequestDTO("newowner@mail.com", 0L);

        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        assertThrows(
            TaskAccessRestrictedException.class,
            () -> taskService.addTaskOwner(1L, request));

        verify(taskBodyRepository, never()).findById(any());
    }

    @Test
    void addTaskOwner_taskNotFound_shouldThrowTaskNotFoundException() {
        AddOwnerRequestDTO request = new AddOwnerRequestDTO("newowner@mail.com", 0L);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
            TaskNotFoundException.class,
            () -> taskService.addTaskOwner(99L, request));
    }

    @Test
    void addTaskOwner_userNotFound_shouldThrowUserNotFoundException() {
        AddOwnerRequestDTO request = new AddOwnerRequestDTO("unknown@mail.com", 0L);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(userFacade.findByEmail("unknown@mail.com")).thenReturn(Optional.empty());

        assertThrows(
            UserNotFoundException.class,
            () -> taskService.addTaskOwner(1L, request));
    }

    @Test
    void addTaskOwner_newOwnerNotOrgOrAdmin_shouldThrowValidationException() {
        AddOwnerRequestDTO request = new AddOwnerRequestDTO("student@mail.com", 0L);

        UserAuthDetails studentUser =
            new UserAuthDetails(300L, "student@mail.com", "12345678", Role.USER);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(userFacade.findByEmail("student@mail.com")).thenReturn(Optional.of(studentUser));

        assertThrows(
            ValidationException.class,
            () -> taskService.addTaskOwner(1L, request));
    }

    @Test
    void addTaskOwner_alreadyOwner_shouldReturnWithoutChanges() {
        AddOwnerRequestDTO request = new AddOwnerRequestDTO("currentowner@mail.com", 0L);

        UserAuthDetails owner =
            new UserAuthDetails(100L, "currentowner@mail.com", "12345678", Role.ORG);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(userFacade.findByEmail("currentowner@mail.com")).thenReturn(Optional.of(owner));
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        int ownersCount = taskBody.getOwners().size();

        TaskResponseDTO result = taskService.addTaskOwner(1L, request);

        assertNotNull(result);
        assertEquals(ownersCount, taskBody.getOwners().size());

        verify(taskBodyMapper).toResponse(taskBody, testFiles, "creator@mail.com");
    }

    // ---- removeTaskOwner ----

    @Test
    void removeTaskOwner_shouldSucceed() {
        TaskOwner secondOwner = TaskOwner.builder()
            .id(new TaskOwnerId(1L, 200L))
            .task(taskBody)
            .build();
        taskBody.addOwner(secondOwner);

        RemoveOwnerRequestDTO request = new RemoveOwnerRequestDTO("currentowner@mail.com", 0L);

        UserAuthDetails owner =
            new UserAuthDetails(100L, "currentowner@mail.com", "12345678", Role.ORG);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(userFacade.findByEmail("currentowner@mail.com")).thenReturn(Optional.of(owner));
        when(taskBodyRepository.saveAndFlush(any(TaskBody.class))).thenReturn(taskBody);
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        TaskResponseDTO result = taskService.removeTaskOwner(1L, request);

        assertNotNull(result);

        assertFalse(taskBody.getOwners().stream()
            .anyMatch(o -> o.getId().getOwnerId().equals(100L)));

        assertTrue(taskBody.getOwners().stream()
            .anyMatch(o -> o.getId().getOwnerId().equals(200L)));

        verify(taskBodyMapper).toResponse(taskBody, testFiles, "creator@mail.com");
    }

    @Test
    void removeTaskOwner_lastOwner_shouldThrowValidationException() {
        RemoveOwnerRequestDTO request = new RemoveOwnerRequestDTO("currentowner@mail.com", 0L);

        UserAuthDetails owner =
            new UserAuthDetails(100L, "currentowner@mail.com", "12345678", Role.ORG);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(userFacade.findByEmail("currentowner@mail.com")).thenReturn(Optional.of(owner));

        assertThrows(ValidationException.class,
            () -> taskService.removeTaskOwner(1L, request));

        assertEquals(1, taskBody.getOwners().size());
        assertTrue(taskBody.getOwners().stream()
            .anyMatch(o -> o.getId().getOwnerId().equals(100L)));
    }

    @Test
    void removeTaskOwner_notAdmin_shouldThrowTaskAccessRestrictedException() {
        RemoveOwnerRequestDTO request = new RemoveOwnerRequestDTO("currentowner@mail.com", 0L);

        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        assertThrows(
            TaskAccessRestrictedException.class,
            () -> taskService.removeTaskOwner(1L, request));

        verify(taskBodyRepository, never()).findById(any());
    }

    @Test
    void removeTaskOwner_taskNotFound_shouldThrowTaskNotFoundException() {
        RemoveOwnerRequestDTO request = new RemoveOwnerRequestDTO("currentowner@mail.com", 0L);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
            TaskNotFoundException.class,
            () -> taskService.removeTaskOwner(99L, request));
    }

    @Test
    void removeTaskOwner_userNotFound_shouldThrowUserNotFoundException() {
        RemoveOwnerRequestDTO request = new RemoveOwnerRequestDTO("currentowner@mail.com", 0L);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(userFacade.findByEmail("currentowner@mail.com")).thenReturn(Optional.empty());

        assertThrows(
            UserNotFoundException.class,
            () -> taskService.removeTaskOwner(1L, request));
    }

    @Test
    void removeTaskOwner_notOwner_shouldReturnWithoutChanges() {
        RemoveOwnerRequestDTO request = new RemoveOwnerRequestDTO("unknown@mail.com", 0L);

        UserAuthDetails user =
            new UserAuthDetails(300L, "unknown@mail.com", "12345678", Role.ORG);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(userFacade.findByEmail("unknown@mail.com")).thenReturn(Optional.of(user));
        when(fileManagerFacade.getFilesByEntity(any(), eq(1L), any())).thenReturn(testFiles);
        when(taskBodyMapper.toResponse(taskBody, testFiles, "creator@mail.com")).thenReturn(taskResponse);

        int ownersCount = taskBody.getOwners().size();

        TaskResponseDTO result = taskService.removeTaskOwner(1L, request);

        assertNotNull(result);
        assertEquals(ownersCount, taskBody.getOwners().size());

        verify(taskBodyMapper).toResponse(taskBody, testFiles, "creator@mail.com");
    }

    // ---- deleteTask ----

    @Test
    void deleteTask_asOwner_shouldDelete() {
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));

        taskService.deleteTask(1L);

        verify(taskBodyRepository).delete(taskBody);
    }

    @Test
    void deleteTask_asAdmin_shouldDelete() {
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(50L));
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskBodyRepository).delete(taskBody);
    }

    @Test
    void deleteTask_nonExistingTask_shouldThrowTaskNotFoundException() {
        when(taskBodyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(99L));

        verify(taskBodyRepository, never()).delete(any());
    }

    @Test
    void deleteTask_notOwnerNotAdmin_shouldThrowTaskAccessRestrictedException() {
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(50L));

        assertThrows(TaskAccessRestrictedException.class, () -> taskService.deleteTask(1L));

        verify(taskBodyRepository, never()).delete(any());
    }

    // ---- version conflict (optimistic locking) ----

    @Test
    void updateTask_staleVersion_shouldThrowStaleTaskVersionException() {
        UpdateTaskRequestDTO request = new UpdateTaskRequestDTO(
            "Updated Title", "Updated Description", null, null, 999L);

        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(100L));

        assertThrows(StaleTaskVersionException.class, () -> taskService.updateTask(1L, request));

        verify(taskBodyRepository, never()).save(any());
    }

    @Test
    void addTaskOwner_staleVersion_shouldThrowStaleTaskVersionException() {
        AddOwnerRequestDTO request = new AddOwnerRequestDTO("newowner@mail.com", 999L);
        UserAuthDetails newOwner =
            new UserAuthDetails(200L, "newowner@mail.com", "12345678", Role.ORG);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(userFacade.findByEmail("newowner@mail.com")).thenReturn(Optional.of(newOwner));

        assertThrows(StaleTaskVersionException.class, () -> taskService.addTaskOwner(1L, request));
    }

    @Test
    void removeTaskOwner_staleVersion_shouldThrowStaleTaskVersionException() {
        RemoveOwnerRequestDTO request = new RemoveOwnerRequestDTO("currentowner@mail.com", 999L);
        UserAuthDetails owner =
            new UserAuthDetails(100L, "currentowner@mail.com", "12345678", Role.ORG);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(taskBodyRepository.findById(1L)).thenReturn(Optional.of(taskBody));
        when(userFacade.findByEmail("currentowner@mail.com")).thenReturn(Optional.of(owner));

        assertThrows(StaleTaskVersionException.class, () -> taskService.removeTaskOwner(1L, request));

        assertEquals(1, taskBody.getOwners().size());
    }
}
