package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.utils.QuestionClaimFailureClassifier;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class AdministratorOfficialAnswerServiceTest {

    private static final String ADMIN_ROLE = "ADMIN";

    private static final Long QUESTION_ID = 10L;
    private static final Long ADMINISTRATOR_ID = 20L;
    private static final Long OTHER_REVIEWER_ID = 30L;
    private static final Long MESSAGE_ID = 40L;

    private static final String QUESTION_CONTENT =
        "Original question content";

    private static final String ANSWER_CONTENT =
        "The memory limit includes the input and output buffers.";

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-01T10:00:00Z");

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionMessageRepository questionMessageRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private QuestionMessageMapper questionMessageMapper;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private QuestionClaimFailureClassifier questionClaimFailureClassifier;

    @InjectMocks
    private AdministratorQuestionServiceImpl administratorQuestionService;

    @ParameterizedTest
    @EnumSource(QuestionStatus.class)
    void publishOfficialAnswer_supportedStatus_shouldCreateServerControlledAnswer(
        QuestionStatus initialStatus) {

        stubAdministrator();

        CreateOfficialAnswerRequestDTO request =
            new CreateOfficialAnswerRequestDTO(
                ANSWER_CONTENT);

        QuestionThread question =
            createQuestion(initialStatus);

        /*
         * Polluted mapper result verifies that every protected message field is
         * overwritten by the service.
         */
        QuestionMessage mappedAnswer =
            QuestionMessage.builder()
                .id(999L)
                .questionThreadId(999L)
                .authorId(999L)
                .type(COMMENT)
                .content(ANSWER_CONTENT)
                .createdAt(CREATED_AT)
                .build();

        QuestionMessage savedAnswer =
            createSavedAnswer(MESSAGE_ID);

        QuestionMessageResponseDTO response =
            createResponse(MESSAGE_ID);

        String originalQuestionContent =
            question.getContent();

        Long originalReviewerId =
            question.getAssignedReviewerId();

        var originalVisibility =
            question.getVisibility();

        when(questionThreadRepository
            .findByIdForUpdate(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionMessageMapper
            .toOfficialAnswerEntity(request))
            .thenReturn(mappedAnswer);

        when(questionMessageRepository.save(mappedAnswer))
            .thenReturn(savedAnswer);

        when(questionMessageMapper.toResponse(savedAnswer))
            .thenReturn(response);

        QuestionMessageResponseDTO result =
            administratorQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    request);

        assertSame(response, result);

        assertAll(
            () -> assertEquals(
                ANSWERED,
                question.getStatus()),
            () -> assertEquals(
                OPEN,
                question.getState()),
            () -> assertEquals(
                originalReviewerId,
                question.getAssignedReviewerId()),
            () -> assertEquals(
                originalVisibility,
                question.getVisibility()),
            () -> assertEquals(
                originalQuestionContent,
                question.getContent()),
            () -> assertEquals(
                null,
                mappedAnswer.getId()),
            () -> assertEquals(
                QUESTION_ID,
                mappedAnswer.getQuestionThreadId()),
            () -> assertEquals(
                ADMINISTRATOR_ID,
                mappedAnswer.getAuthorId()),
            () -> assertEquals(
                OFFICIAL_ANSWER,
                mappedAnswer.getType()),
            () -> assertEquals(
                null,
                mappedAnswer.getCreatedAt()),
            () -> assertEquals(
                ANSWER_CONTENT,
                mappedAnswer.getContent()));

        InOrder order = inOrder(
            questionThreadRepository,
            questionMessageMapper,
            questionMessageRepository);

        order.verify(questionThreadRepository)
            .findByIdForUpdate(QUESTION_ID);

        order.verify(questionMessageMapper)
            .toOfficialAnswerEntity(request);

        order.verify(questionMessageRepository)
            .save(mappedAnswer);

        order.verify(questionMessageMapper)
            .toResponse(savedAnswer);

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));

        verifyNoInteractions(
            questionThreadMapper,
            questionClaimFailureClassifier);
    }

    @Test
    void publishOfficialAnswer_answeredQuestion_shouldAllowMultipleAnswers() {
        stubAdministrator();

        CreateOfficialAnswerRequestDTO firstRequest =
            new CreateOfficialAnswerRequestDTO(
                "First official answer");

        CreateOfficialAnswerRequestDTO secondRequest =
            new CreateOfficialAnswerRequestDTO(
                "Second official answer");

        QuestionThread question =
            createQuestion(ANSWERED);

        QuestionMessage firstMapped =
            QuestionMessage.builder()
                .content(firstRequest.content())
                .build();

        QuestionMessage secondMapped =
            QuestionMessage.builder()
                .content(secondRequest.content())
                .build();

        QuestionMessage firstSaved =
            createSavedAnswer(41L);

        firstSaved.setContent(firstRequest.content());

        QuestionMessage secondSaved =
            createSavedAnswer(42L);

        secondSaved.setContent(secondRequest.content());

        when(questionThreadRepository
            .findByIdForUpdate(QUESTION_ID))
            .thenReturn(
                Optional.of(question),
                Optional.of(question));

        when(questionMessageMapper
            .toOfficialAnswerEntity(firstRequest))
            .thenReturn(firstMapped);

        when(questionMessageMapper
            .toOfficialAnswerEntity(secondRequest))
            .thenReturn(secondMapped);

        when(questionMessageRepository.save(firstMapped))
            .thenReturn(firstSaved);

        when(questionMessageRepository.save(secondMapped))
            .thenReturn(secondSaved);

        when(questionMessageMapper.toResponse(firstSaved))
            .thenReturn(new QuestionMessageResponseDTO(
                41L,
                QUESTION_ID,
                ADMINISTRATOR_ID,
                OFFICIAL_ANSWER,
                firstRequest.content(),
                CREATED_AT));

        when(questionMessageMapper.toResponse(secondSaved))
            .thenReturn(new QuestionMessageResponseDTO(
                42L,
                QUESTION_ID,
                ADMINISTRATOR_ID,
                OFFICIAL_ANSWER,
                secondRequest.content(),
                CREATED_AT));

        administratorQuestionService.publishOfficialAnswer(
            QUESTION_ID,
            firstRequest);

        administratorQuestionService.publishOfficialAnswer(
            QUESTION_ID,
            secondRequest);

        assertEquals(
            ANSWERED,
            question.getStatus());

        verify(questionMessageRepository)
            .save(firstMapped);

        verify(questionMessageRepository)
            .save(secondMapped);
    }

    @Test
    void publishOfficialAnswer_closedQuestion_shouldRejectBeforeMappingAndSave() {
        stubAdministrator();

        QuestionThread question =
            createQuestion(ANSWERED);

        question.setState(CLOSED);

        when(questionThreadRepository
            .findByIdForUpdate(QUESTION_ID))
            .thenReturn(Optional.of(question));

        assertThrows(
            InvalidQuestionStateException.class,
            () -> administratorQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    createRequest()));

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper,
            questionThreadMapper,
            questionClaimFailureClassifier);
    }

    @Test
    void publishOfficialAnswer_missingQuestion_shouldReturnNotFound() {
        stubAdministrator();

        when(questionThreadRepository
            .findByIdForUpdate(QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> administratorQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    createRequest()));

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper,
            questionThreadMapper,
            questionClaimFailureClassifier);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void publishOfficialAnswer_invalidQuestionId_shouldRejectBeforeSecurity(
        Long questionId) {

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .publishOfficialAnswer(
                    questionId,
                    createRequest()));

        verifyNoInteractions(
            securityFacade,
            questionThreadRepository,
            questionMessageRepository,
            questionMessageMapper,
            questionThreadMapper,
            questionClaimFailureClassifier);
    }

    @Test
    void publishOfficialAnswer_unauthenticated_shouldRejectBeforeRepository() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> administratorQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    createRequest()));

        verify(securityFacade, never())
            .hasRole(ADMIN_ROLE);

        verifyNoInteractions(
            questionThreadRepository,
            questionMessageRepository,
            questionMessageMapper,
            questionThreadMapper,
            questionClaimFailureClassifier);
    }

    @Test
    void publishOfficialAnswer_nonAdministrator_shouldRejectBeforeRepository() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(ADMINISTRATOR_ID));

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> administratorQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    createRequest()));

        verifyNoInteractions(
            questionThreadRepository,
            questionMessageRepository,
            questionMessageMapper,
            questionThreadMapper,
            questionClaimFailureClassifier);
    }

    @Test
    void publishOfficialAnswer_shouldUseWritableTransaction()
        throws NoSuchMethodException {

        Method method =
            AdministratorQuestionServiceImpl.class
                .getMethod(
                    "publishOfficialAnswer",
                    Long.class,
                    CreateOfficialAnswerRequestDTO.class);

        Transactional transactional =
            AnnotatedElementUtils.findMergedAnnotation(
                method,
                Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    @Test
    void findByIdForUpdate_shouldUsePessimisticWriteLock()
        throws NoSuchMethodException {

        Method method =
            QuestionThreadRepository.class
                .getMethod(
                    "findByIdForUpdate",
                    Long.class);

        Lock lock =
            method.getAnnotation(Lock.class);

        assertNotNull(lock);
        assertEquals(
            PESSIMISTIC_WRITE,
            lock.value());
    }

    private void stubAdministrator() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(ADMINISTRATOR_ID));

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(true);
    }

    private CreateOfficialAnswerRequestDTO createRequest() {
        return new CreateOfficialAnswerRequestDTO(
            ANSWER_CONTENT);
    }

    private QuestionThread createQuestion(
        QuestionStatus status) {

        return QuestionThread.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(100L)
            .authorId(200L)
            .assignedReviewerId(OTHER_REVIEWER_ID)
            .title("Question title")
            .content(QUESTION_CONTENT)
            .status(status)
            .state(OPEN)
            .visibility(PRIVATE)
            .version(3L)
            .createdAt(CREATED_AT)
            .updatedAt(CREATED_AT)
            .build();
    }

    private QuestionMessage createSavedAnswer(
        Long messageId) {

        return QuestionMessage.builder()
            .id(messageId)
            .questionThreadId(QUESTION_ID)
            .authorId(ADMINISTRATOR_ID)
            .type(OFFICIAL_ANSWER)
            .content(ANSWER_CONTENT)
            .createdAt(CREATED_AT)
            .build();
    }

    private QuestionMessageResponseDTO createResponse(
        Long messageId) {

        return new QuestionMessageResponseDTO(
            messageId,
            QUESTION_ID,
            ADMINISTRATOR_ID,
            OFFICIAL_ANSWER,
            ANSWER_CONTENT,
            CREATED_AT);
    }
}