package com.itasocialacademy.oitassist.task.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.task.dto.request.ChangeOwnerRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.UpdateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import com.itasocialacademy.oitassist.task.exceptions.TaskAccessRestrictedException;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.task.service.interfaces.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import java.util.List;

class TaskControllerTest extends ControllerUnitTest<TaskController> {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    @Override
    protected TaskController getController() {
        return taskController;
    }

    private TaskResponseDTO mockTaskResponse;

    @BeforeEach
    void setUpMockData() {
        mockTaskResponse = TaskResponseDTO.builder()
            .id(1L)
            .title("PowerPoint Різдвяна зірка")
            .description("Створити у файлі-розв'язку на одному слайді")
            .createdBy(100L)
            .ownerId(100L)
            .build();
    }

    // ---- createTask ----

    @Test
    void createTask_validRequest_shouldReturn201() throws Exception {
        CreateTaskRequestDTO request = new CreateTaskRequestDTO(
            "PowerPoint Різдвяна зірка",
            "Створити у файлі-розв'язку на одному слайді",
            List.of(51L, 52L));

        when(taskService.createTask(any(CreateTaskRequestDTO.class))).thenReturn(mockTaskResponse);

        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.title").value("PowerPoint Різдвяна зірка"))
            .andExpect(jsonPath("$.ownerId").value(100L));

        verify(taskService).createTask(any(CreateTaskRequestDTO.class));
    }

    @Test
    void createTask_roleRestricted_shouldReturn403() throws Exception {
        CreateTaskRequestDTO request = new CreateTaskRequestDTO(
            "Some Task", "Description", List.of(1L));

        when(taskService.createTask(any(CreateTaskRequestDTO.class)))
            .thenThrow(new TaskAccessRestrictedException(0L));

        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void createTask_blankTitle_shouldReturn400() throws Exception {
        CreateTaskRequestDTO request = new CreateTaskRequestDTO(
            "",
            "Some description",
            List.of(1L));

        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_emptyFileIds_shouldReturn400() throws Exception {
        CreateTaskRequestDTO request = new CreateTaskRequestDTO(
            "Valid Title",
            "Some description",
            List.of());

        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ---- getTask ----

    @Test
    void getTask_existingId_shouldReturn200() throws Exception {
        when(taskService.getTaskById(1L)).thenReturn(mockTaskResponse);

        mockMvc.perform(get("/api/v1/tasks/{taskId}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.title").value("PowerPoint Різдвяна зірка"))
            .andExpect(jsonPath("$.description").value("Створити у файлі-розв'язку на одному слайді"));

        verify(taskService).getTaskById(1L);
    }

    @Test
    void getTask_nonExistingId_shouldReturn404() throws Exception {
        when(taskService.getTaskById(99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/v1/tasks/{taskId}", 99L))
            .andExpect(status().isNotFound());
    }

    // ---- getAllTasks ----

    @Test
    void getAllTasks_asAdmin_shouldReturnPageResponseAnd200() throws Exception {
        Page<TaskResponseDTO> page = new PageImpl<>(List.of(mockTaskResponse));
        when(taskService.getAllTasks(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/tasks")
                .param("page", "0")
                .param("size", "15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1L))
            .andExpect(jsonPath("$.content[0].title").value("PowerPoint Різдвяна зірка"))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(taskService).getAllTasks(any(Pageable.class));
    }

    @Test
    void getAllTasks_nonAdmin_shouldReturn403() throws Exception {
        when(taskService.getAllTasks(any(Pageable.class)))
            .thenThrow(new TaskAccessRestrictedException(0L));

        mockMvc.perform(get("/api/v1/tasks")
                .param("page", "0")
                .param("size", "15"))
            .andExpect(status().isForbidden());
    }

    // ---- getMyTasks ----

    @Test
    void getMyTasks_shouldReturnPageResponseAnd200() throws Exception {
        Page<TaskResponseDTO> page = new PageImpl<>(List.of(mockTaskResponse));
        when(taskService.getAllMyTasks(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/tasks/my")
                .param("page", "0")
                .param("size", "15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1L))
            .andExpect(jsonPath("$.content[0].ownerId").value(100L))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(taskService).getAllMyTasks(any(Pageable.class));
    }

    // ---- updateTask ----

    @Test
    void updateTask_validRequest_shouldReturn200() throws Exception {
        UpdateTaskRequestDTO request = new UpdateTaskRequestDTO(
            "Оновлена назва завдання",
            "Оновлений опис завдання",
            List.of(51L, 62L),
            List.of(52L));

        TaskResponseDTO updatedResponse = TaskResponseDTO.builder()
            .id(1L)
            .title("Оновлена назва завдання")
            .description("Оновлений опис завдання")
            .createdBy(100L)
            .ownerId(100L)
            .build();

        when(taskService.updateTask(eq(1L), any(UpdateTaskRequestDTO.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/tasks/{taskId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.title").value("Оновлена назва завдання"))
            .andExpect(jsonPath("$.description").value("Оновлений опис завдання"));

        verify(taskService).updateTask(eq(1L), any(UpdateTaskRequestDTO.class));
    }

    @Test
    void updateTask_notOwnerNotAdmin_shouldReturn403() throws Exception {
        UpdateTaskRequestDTO request = new UpdateTaskRequestDTO(
            "Title", "Description", null, null);

        when(taskService.updateTask(eq(1L), any(UpdateTaskRequestDTO.class)))
            .thenThrow(new TaskAccessRestrictedException(1L));

        mockMvc.perform(put("/api/v1/tasks/{taskId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateTask_taskNotFound_shouldReturn404() throws Exception {
        UpdateTaskRequestDTO request = new UpdateTaskRequestDTO(
            "Title", "Description", null, null);

        when(taskService.updateTask(eq(99L), any(UpdateTaskRequestDTO.class)))
            .thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(put("/api/v1/tasks/{taskId}", 99L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateTask_blankTitle_shouldReturn400() throws Exception {
        UpdateTaskRequestDTO request = new UpdateTaskRequestDTO(
            "",
            "Оновлений опис завдання",
            null,
            null);

        mockMvc.perform(put("/api/v1/tasks/{taskId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ---- changeOwner ----

    @Test
    void changeOwner_validRequest_shouldReturn200() throws Exception {
        ChangeOwnerRequestDTO request = new ChangeOwnerRequestDTO("newowner@mail.com");

        TaskResponseDTO changedOwnerResponse = TaskResponseDTO.builder()
            .id(1L)
            .title("PowerPoint Різдвяна зірка")
            .description("Створити у файлі-розв'язку на одному слайді")
            .createdBy(100L)
            .ownerId(200L)
            .build();

        when(taskService.changeTaskOwner(eq(1L), any(ChangeOwnerRequestDTO.class)))
            .thenReturn(changedOwnerResponse);

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/change-owner", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.ownerId").value(200L));

        verify(taskService).changeTaskOwner(eq(1L), any(ChangeOwnerRequestDTO.class));
    }

    @Test
    void changeOwner_nonAdmin_shouldReturn403() throws Exception {
        ChangeOwnerRequestDTO request = new ChangeOwnerRequestDTO("newowner@mail.com");

        when(taskService.changeTaskOwner(eq(1L), any(ChangeOwnerRequestDTO.class)))
            .thenThrow(new TaskAccessRestrictedException(1L));

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/change-owner", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void changeOwner_invalidEmail_shouldReturn400() throws Exception {
        ChangeOwnerRequestDTO request = new ChangeOwnerRequestDTO("not-an-email");

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/change-owner", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changeOwner_blankEmail_shouldReturn400() throws Exception {
        ChangeOwnerRequestDTO request = new ChangeOwnerRequestDTO("");

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/change-owner", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ---- deleteTask ----

    @Test
    void deleteTask_shouldReturn204() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/api/v1/tasks/{taskId}", 1L))
            .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L);
    }

    @Test
    void deleteTask_notOwnerNotAdmin_shouldReturn403() throws Exception {
        doThrow(new TaskAccessRestrictedException(1L)).when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/api/v1/tasks/{taskId}", 1L))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteTask_taskNotFound_shouldReturn404() throws Exception {
        doThrow(new TaskNotFoundException(99L)).when(taskService).deleteTask(99L);

        mockMvc.perform(delete("/api/v1/tasks/{taskId}", 99L))
            .andExpect(status().isNotFound());
    }
}

