package com.itasocialacademy.oitassist.users.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.user.controller.UserController;
import com.itasocialacademy.oitassist.user.dao.dto.request.ProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.ReviewRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.exceptions.InvalidSortFieldException;
import com.itasocialacademy.oitassist.user.exceptions.ProfileUpdateRequestException;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.*;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Unit tests for Users Controller")
@EnableSpringDataWebSupport
class UsersControllerTest extends ControllerUnitTest<UserController> {
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Override
    protected UserController getController() {
        return userController;
    }

    @Test
    @DisplayName("GET /profile should return 200 with user profile when authenticated")
    void getProfile_ShouldReturnOkWithBody_IfUserIsAuthenticated() throws Exception {
        // given
        ResponseUserDTO profile = ResponseUserDTO.builder()
            .id(1L)
            .email("test@email.com")
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .role(Role.USER)
            .status(UserStatus.ACTIVE)
            .build();

        // when
        when(userService.getCurrentUserProfile()).thenReturn(profile);

        mockMvc.perform(
            get("/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.email").value("test@email.com"))
            .andExpect(jsonPath("$.middleName").value("John"))
            .andExpect(jsonPath("$.phoneNumber").value("380931111111"))
            .andExpect(jsonPath("$.firstName").value("Bob"))
            .andExpect(jsonPath("$.lastName").value("Smith"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService, times(1)).getCurrentUserProfile();
    }

    @Test
    @DisplayName("Post /profile/update-request should return 201 when authenticated")
    void createProfileUpdateRequest_ShouldReturnCreated_whenRequestIsValid() throws Exception {
        // given
        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        // when
        doNothing().when(userService).createProfileUpdateRequest(request);

        mockMvc.perform(
            post("/api/v1/users/profile/update-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // then
            .andExpect(status().isCreated());

        verify(userService, times(1)).createProfileUpdateRequest(request);
    }

    @Test
    @DisplayName("Post /profile/update-request should throw ProfileUpdateRequestException when some request has status pending")
    void createProfileUpdateRequest_shouldThrow_whenPendingExists() throws Exception {
        // given
        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        // when
        doThrow(new ProfileUpdateRequestException("User already have a pending update request",
            ErrorCode.PROFILE_UPDATE_REQUEST_ALREADY_PENDING))
            .when(userService).createProfileUpdateRequest(request);

        mockMvc.perform(
            post("/api/v1/users/profile/update-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // then
            .andExpect(status().isConflict());

        verify(userService, times(1)).createProfileUpdateRequest(request);
    }

    @Test
    @DisplayName("Post /profile/update-request should throw ProfileUpdateRequestException when user already had an update request during the day")
    void createProfileUpdateRequest_shouldThrow_whenAlreadyHadARequestToday() throws Exception {
        // given
        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .firstName("Bob")
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        // when
        doThrow(new ProfileUpdateRequestException("User already had a request today",
            ErrorCode.PROFILE_UPDATE_REQUEST_DAILY_LIMIT))
            .when(userService).createProfileUpdateRequest(request);

        mockMvc.perform(
            post("/api/v1/users/profile/update-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // then
            .andExpect(status().isConflict());

        verify(userService, times(1)).createProfileUpdateRequest(request);
    }

    @Test
    @DisplayName("Post /profile/update-request should return 400 when body is invalid")
    void createProfileUpdateRequest_shouldReturn400_whenBodyIsIncorrect() throws Exception {
        // given
        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
            .lastName("Smith")
            .middleName("John")
            .phoneNumber("380931111111")
            .build();

        // when
        mockMvc.perform(
            post("/api/v1/users/profile/update-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // then
            .andExpect(status().isBadRequest());

        verify(userService, never()).createProfileUpdateRequest(any());
    }

    @Test
    @DisplayName("GET /profile/update-request should return 200 with paginated list when ORG role")
    void getProfileUpdateRequests_ShouldReturnCreated_whenRequestIsValid() throws Exception {
        // given
        UpdateRequestStatus status = UpdateRequestStatus.PENDING;

        ResponseProfileUpdateRequestDTO requestDTO = ResponseProfileUpdateRequestDTO.builder()
            .id(1L)
            .status(UpdateRequestStatus.PENDING)
            .oldFirstName("Bob")
            .oldLastName("Smith")
            .oldMiddleName("John")
            .oldPhoneNumber("380931111111")
            .newFirstName("Alice")
            .newLastName("Johnson")
            .newMiddleName("Marie")
            .newPhoneNumber("380932222222")
            .requestedAt(Instant.parse("2024-01-01T00:00:00Z"))
            .build();

        Page<ResponseProfileUpdateRequestDTO> page = new PageImpl<>(
            List.of(requestDTO),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "requestedAt")),
            1);

        // when
        when(userService.getProfileUpdateRequests(eq(status), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(
            get("/api/v1/users/profile/update-request")
                .param("status", "PENDING")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1L))
            .andExpect(jsonPath("$.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.content[0].oldFirstName").value("Bob"))
            .andExpect(jsonPath("$.content[0].oldLastName").value("Smith"))
            .andExpect(jsonPath("$.content[0].newFirstName").value("Alice"))
            .andExpect(jsonPath("$.content[0].newLastName").value("Johnson"))
            .andExpect(jsonPath("$.content[0].requestedAt").value("2024-01-01T00:00:00Z"))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(userService, times(1)).getProfileUpdateRequests(eq(status), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /profile/update-request should return 400, if status is invalid ")
    void getProfileUpdateRequests_ShouldReturn400_whenStatusIsWrong() throws Exception {
        // when & then
        mockMvc.perform(
            get("/api/v1/users/profile/update-request")
                .param("status", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("GET /profile/update-request - should return 400 when sort field is invalid")
    void getProfileUpdateRequests_ShouldReturn400_whenSortingIsWrong() throws Exception {
        // when
        when(userService.getProfileUpdateRequests(any(), any(Pageable.class)))
            .thenThrow(new InvalidSortFieldException("invalidField"));

        mockMvc.perform(
            get("/api/v1/users/profile/update-request")
                .param("status", "PENDING")
                .param("sort", "invalidField,desc")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /profile/update-request/{id}/review - should return 204 when request is valid")
    void reviewProfileUpdateRequests_ShouldReturn204_whenRequestIsValid() throws Exception {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.APPROVED, null);

        doNothing().when(userService).reviewProfileUpdateRequests(id, body);

        mockMvc.perform(
            patch("/api/v1/users/profile/update-request/{id}/review", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNoContent());

        verify(userService, times(1)).reviewProfileUpdateRequests(id, body);
    }

    @Test
    @DisplayName("PATCH /profile/update-request/{id}/review - should return 404 when request not found")
    void reviewProfileUpdateRequests_ShouldReturn404_whenProfileUpdateRequestsNotFound() throws Exception {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.APPROVED, null);

        doThrow(new EntityNotFoundException("Request not found: " + id))
            .when(userService).reviewProfileUpdateRequests(id, body);

        mockMvc.perform(
            patch("/api/v1/users/profile/update-request/{id}/review", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound());

        verify(userService, times(1)).reviewProfileUpdateRequests(id, body);
    }

    @Test
    @DisplayName("PATCH /profile/update-request/{id}/review - should return 404 when user not found")
    void reviewProfileUpdateRequests_ShouldReturn404_whenUserNotFound() throws Exception {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.APPROVED, null);

        doThrow(new EntityNotFoundException("User not found"))
            .when(userService).reviewProfileUpdateRequests(id, body);

        // when & then
        mockMvc.perform(
            patch("/api/v1/users/profile/update-request/{id}/review", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound());

        verify(userService, times(1)).reviewProfileUpdateRequests(id, body);
    }

    @Test
    @DisplayName("PATCH /profile/update-request/{id}/review - should return 409 when request already reviewed")
    void reviewProfileUpdateRequests_ShouldReturn409_whenRequestAlreadyReviewed() throws Exception {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.APPROVED, null);

        doThrow(new ProfileUpdateRequestException("Request is already reviewed",
            ErrorCode.PROFILE_UPDATE_REQUEST_ALREADY_REVIEWED))
            .when(userService).reviewProfileUpdateRequests(id, body);

        // when & then
        mockMvc.perform(
            patch("/api/v1/users/profile/update-request/{id}/review", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict());

        verify(userService, times(1)).reviewProfileUpdateRequests(id, body);
    }

    @Test
    @DisplayName("PATCH /profile/update-request/{id}/review - should return 400 when reject reason is blank")
    void reviewProfileUpdateRequests_ShouldReturn400_whenRejectReasonIsBlank() throws Exception {
        // given
        Long id = 1L;
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.REJECTED, null);

        doThrow(new ProfileUpdateRequestException("Rejection reason cannot be blank",
            ErrorCode.COMMON_VALIDATION_FAILED))
            .when(userService).reviewProfileUpdateRequests(id, body);

        // when & then
        mockMvc.perform(
            patch("/api/v1/users/profile/update-request/{id}/review", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());

        verify(userService, times(1)).reviewProfileUpdateRequests(id, body);
    }

    @Test
    @DisplayName("PATCH /profile/update-request/{id}/review - should return 400 when rejectReason exceeds 500 characters")
    void reviewProfileUpdateRequests_ShouldReturn400_whenRejectReasonIsTooLong() throws Exception {
        // given
        Long id = 1L;
        String longReason = "a".repeat(501);
        ReviewRequestDTO body = new ReviewRequestDTO(UpdateRequestStatus.REJECTED, longReason);

        // when & then
        mockMvc.perform(
            patch("/api/v1/users/profile/update-request/{id}/review", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

}