package com.itasocialacademy.oitassist.chat.controller;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import com.itasocialacademy.oitassist.chat.service.interfaces.ParticipantForumService;
import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/questions")
@RequiredArgsConstructor
@Tag(
    name = "Participant Forum V1",
    description = "Participant operations for task-scoped question forums")
public class ParticipantForumController {
    private final ParticipantForumService participantForumService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<QuestionThreadSummaryResponseDTO>> getParticipantForum(
        @PathVariable Long taskId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            PageResponse.from(
                participantForumService.getForumQuestions(
                    taskId,
                    page,
                    size)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuestionThreadResponseDTO> createQuestion(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateQuestionRequestDTO request
    ) {
        QuestionThreadResponseDTO response =
                participantForumService.createQuestion(taskId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
