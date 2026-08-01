package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
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

        /*
         * The question and its access policy must be resolved before message content is
         * requested from the repository.
         */
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

    private QuestionThread loadAuthorizedQuestion(
        Long questionId) {
        QuestionThread question = questionThreadRepository
            .findById(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(questionId));

        questionAccessPolicy.requireQuestionViewAccess(question);

        return question;
    }

    private void validateQuestionId(Long questionId) {
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