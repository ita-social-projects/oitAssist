package com.itasocialacademy.oitassist.chat.service;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.ParticipantForumService;
import com.itasocialacademy.oitassist.chat.utils.QuestionAccessPolicy;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantForumServiceImpl implements ParticipantForumService {
    private static final int MAX_PAGE_SIZE = 100;

    private static final Sort FORUM_SORT = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id"));

    private final QuestionThreadRepository questionThreadRepository;
    private final QuestionThreadMapper questionThreadMapper;
    private final QuestionAccessPolicy questionAccessPolicy;

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionThreadSummaryResponseDTO> getForumQuestions(
        Long taskAssignmentId,
        int page,
        int size) {
        validateRequest(taskAssignmentId, page, size);

        Long participantId = questionAccessPolicy.requireTaskAssignmentForumAccess(taskAssignmentId);

        Pageable pageable = PageRequest.of(
            page,
            size,
            FORUM_SORT);

        return questionThreadRepository.findParticipantVisibleQuestions(
            taskAssignmentId,
            participantId,
            pageable)
            .map(questionThreadMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public QuestionThreadResponseDTO createQuestion(
        Long taskAssignmentId,
        CreateQuestionRequestDTO request) {
        validateTaskAssignmentId(taskAssignmentId);

        Long authorId = questionAccessPolicy.requireTaskAssignmentQuestionCreationAccess(taskAssignmentId);

        QuestionThread question = questionThreadMapper.toEntity(request);

        question.setTaskAssignmentId(taskAssignmentId);
        question.setAuthorId(authorId);
        question.setStatus(QuestionStatus.NEW);
        question.setState(QuestionState.OPEN);
        question.setVisibility(QuestionVisibility.PRIVATE);
        question.setAssignedReviewerId(null);

        QuestionThread savedQuestion = questionThreadRepository.save(question);

        return questionThreadMapper.toResponse(savedQuestion);
    }

    private void validateRequest(
        Long taskAssignmentId,
        int page,
        int size) {
        validateTaskAssignmentId(taskAssignmentId);

        if (page < 0) {
            throw new ValidationException(
                "Page number must not be negative",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException(
                "Page size must be between 1 and %d"
                    .formatted(MAX_PAGE_SIZE),
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateTaskAssignmentId(Long taskAssignmentId) {
        if (taskAssignmentId == null || taskAssignmentId <= 0) {
            throw new ValidationException(
                "Task assignment id must be a positive number",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }
}