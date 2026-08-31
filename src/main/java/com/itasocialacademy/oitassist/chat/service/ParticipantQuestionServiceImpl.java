package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateCommentRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.ParticipantQuestionService;
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
public class ParticipantQuestionServiceImpl
    implements ParticipantQuestionService {
    private static final Sort MESSAGE_HISTORY_SORT = Sort.by(
        Sort.Order.asc("createdAt"),
        Sort.Order.asc("id"));

    private final QuestionThreadRepository questionThreadRepository;
    private final QuestionMessageRepository questionMessageRepository;
    private final QuestionThreadMapper questionThreadMapper;
    private final QuestionMessageMapper questionMessageMapper;
    private final QuestionAccessPolicy questionAccessPolicy;

    @Override
    @Transactional(readOnly = true)
    public QuestionThreadResponseDTO getQuestionDetails(
        Long questionId) {
        log.debug(
            "Retrieving participant question details: questionId={}",
            questionId);

        validateQuestionId(questionId);

        QuestionThread question =
            loadAuthorizedQuestion(questionId);

        return questionThreadMapper.toResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionMessageResponseDTO> getQuestionMessages(
        Long questionId,
        int page,
        int size) {
        log.debug(
            "Retrieving participant question messages: "
                + "questionId={}, page={}, size={}",
            questionId,
            page,
            size);

        validateQuestionId(questionId);
        validatePageAndSize(page, size);

        loadAuthorizedQuestion(questionId);

        Pageable pageable = PageRequest.of(
            page,
            size,
            MESSAGE_HISTORY_SORT);

        Page<QuestionMessageResponseDTO> result =
            questionMessageRepository
                .findAllByQuestionThreadId(
                    questionId,
                    pageable)
                .map(questionMessageMapper::toResponse);

        log.debug(
            "Participant question messages retrieved: "
                + "questionId={}, page={}, returnedElements={}, totalElements={}",
            questionId,
            page,
            result.getNumberOfElements(),
            result.getTotalElements());

        return result;
    }

    @Override
    @Transactional
    public QuestionMessageResponseDTO addComment(
        Long questionId,
        CreateCommentRequestDTO request) {
        log.debug(
            "Creating participant comment: questionId={}",
            questionId);

        validateQuestionId(questionId);

        QuestionThread question =
            loadQuestion(questionId);

        /*
         * Access is checked before state validation and persistence so that protected
         * question information is not exposed to unauthorized users.
         */
        Long authorId =
            questionAccessPolicy
                .requireQuestionCommentAccess(question);

        validateQuestionAcceptsComments(question);

        QuestionMessage comment =
            questionMessageMapper.toEntity(request);

        /*
         * All fields except content are controlled by the backend.
         */
        comment.setId(null);
        comment.setAuthorId(authorId);
        comment.setQuestionThreadId(questionId);
        comment.setType(COMMENT);
        comment.setCreatedAt(null);

        QuestionMessage savedComment =
            questionMessageRepository.save(comment);

        log.info(
            "Participant comment created: "
                + "messageId={}, questionId={}, authorId={}",
            savedComment.getId(),
            questionId,
            authorId);

        return questionMessageMapper.toResponse(
            savedComment);
    }

    private QuestionThread loadAuthorizedQuestion(
        Long questionId) {
        QuestionThread question =
            loadQuestion(questionId);

        questionAccessPolicy
            .requireQuestionViewAccess(question);

        return question;
    }

    private QuestionThread loadQuestion(
        Long questionId) {
        return questionThreadRepository
            .findById(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(
                questionId));
    }

    private void validateQuestionAcceptsComments(
        QuestionThread question) {
        if (question.getState() != OPEN) {
            throw new InvalidQuestionStateException(
                question.getId(),
                question.getState(),
                "add comment");
        }
    }

    private void validateQuestionId(
        Long questionId) {
        if (questionId == null || questionId <= 0) {
            throw new ValidationException(
                "Question id must be a positive number",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validatePageAndSize(
        int page,
        int size) {
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
}