package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.config.StompAuthorizationChannelInterceptor;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class StompAuthorizationChannelInterceptorTest {

    private static final Long TASK_ASSIGNMENT_ID = 12L;
    private static final Long QUESTION_ID = 84L;

    @Mock
    private QuestionAccessPolicy questionAccessPolicy;

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    private StompAuthorizationChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {

        interceptor =
            new StompAuthorizationChannelInterceptor(
                new RealtimeSubscriptionDestinationParser(),
                questionAccessPolicy,
                questionThreadRepository);
    }

    @Test
    void subscribe_taskAssignmentForum_shouldDelegateToPolicy() {

        Message<?> message =
            subscription(
                "/topic/task-assignments/12/questions",
                authenticatedUser());

        assertDoesNotThrow(
            () -> interceptor.preSend(
                message,
                null));

        verify(questionAccessPolicy)
            .requireTaskAssignmentForumAccess(
                TASK_ASSIGNMENT_ID);

        verifyNoInteractions(
            questionThreadRepository);
    }

    @Test
    void subscribe_publicQuestion_shouldDelegateToPolicy() {

        QuestionThread question =
            QuestionThread.builder()
                .id(QUESTION_ID)
                .visibility(PUBLIC)
                .build();

        when(questionThreadRepository
            .findById(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        Message<?> message =
            subscription(
                "/topic/questions/84",
                authenticatedUser());

        assertDoesNotThrow(
            () -> interceptor.preSend(
                message,
                null));

        verify(questionAccessPolicy)
            .requireQuestionViewAccess(
                question);
    }

    @Test
    void subscribe_privateQuestion_shouldRejectWithoutDisclosure() {

        QuestionThread question =
            QuestionThread.builder()
                .id(QUESTION_ID)
                .visibility(PRIVATE)
                .build();

        when(questionThreadRepository
            .findById(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        Message<?> message =
            subscription(
                "/topic/questions/84",
                authenticatedUser());

        assertThrows(
            AccessDeniedException.class,
            () -> interceptor.preSend(
                message,
                null));

        verifyNoInteractions(
            questionAccessPolicy);
    }

    @Test
    void subscribe_inaccessiblePublicQuestion_shouldRejectGenerically() {

        QuestionThread question =
            QuestionThread.builder()
                .id(QUESTION_ID)
                .visibility(PUBLIC)
                .build();

        when(questionThreadRepository
            .findById(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        doThrow(
            new QuestionNotFoundException(
                QUESTION_ID))
            .when(questionAccessPolicy)
            .requireQuestionViewAccess(
                question);

        Message<?> message =
            subscription(
                "/topic/questions/84",
                authenticatedUser());

        assertThrows(
            AccessDeniedException.class,
            () -> interceptor.preSend(
                message,
                null));
    }

    @Test
    void subscribe_adminInbox_asAdministrator_shouldAllow() {

        when(questionAccessPolicy
            .isAdministrator())
            .thenReturn(true);

        Message<?> message =
            subscription(
                "/topic/admin/questions/inbox",
                authenticatedUser());

        assertDoesNotThrow(
            () -> interceptor.preSend(
                message,
                null));
    }

    @Test
    void subscribe_adminInbox_asNonAdministrator_shouldReject() {

        when(questionAccessPolicy
            .isAdministrator())
            .thenReturn(false);

        Message<?> message =
            subscription(
                "/topic/admin/questions/inbox",
                authenticatedUser());

        assertThrows(
            AccessDeniedException.class,
            () -> interceptor.preSend(
                message,
                null));
    }

    @Test
    void subscribe_personalQuestions_asAuthenticatedUser_shouldAllow() {

        Message<?> message =
            subscription(
                "/user/queue/questions",
                authenticatedUser());

        assertDoesNotThrow(
            () -> interceptor.preSend(
                message,
                null));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository);
    }

    @Test
    void subscribe_personalReviews_asAdministrator_shouldRemainAllowed() {

        when(questionAccessPolicy
            .isAdministrator())
            .thenReturn(true);

        Message<?> message =
            subscription(
                "/user/queue/reviews",
                authenticatedUser());

        assertDoesNotThrow(
            () -> interceptor.preSend(
                message,
                null));

        verifyNoInteractions(
            questionThreadRepository);
    }

    @Test
    void subscribe_personalReviews_asOrg_shouldAllow() {

        Message<?> message =
            subscription(
                "/user/queue/reviews",
                authenticatedUser(
                    "ROLE_ORG"));

        assertDoesNotThrow(
            () -> interceptor.preSend(
                message,
                null));

        verifyNoInteractions(
            questionThreadRepository);
    }

    @Test
    void subscribe_personalReviews_asRegularUser_shouldReject() {

        Message<?> message =
            subscription(
                "/user/queue/reviews",
                authenticatedUser(
                    "ROLE_USER"));

        assertThrows(
            AccessDeniedException.class,
            () -> interceptor.preSend(
                message,
                null));

        verifyNoInteractions(
            questionThreadRepository);
    }

    @Test
    void subscribe_personalReviews_withoutAuthentication_shouldReject() {

        Message<?> message =
            subscription(
                "/user/queue/reviews",
                null);

        assertThrows(
            AuthenticationCredentialsNotFoundException.class,
            () -> interceptor.preSend(
                message,
                null));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository);
    }

    @Test
    void subscribe_anotherUsersResolvedReviewsQueue_shouldReject() {

        Message<?> message =
            subscription(
                "/user/42/queue/reviews",
                authenticatedUser());

        assertThrows(
            AccessDeniedException.class,
            () -> interceptor.preSend(
                message,
                null));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository);
    }

    @Test
    void subscribe_withoutAuthentication_shouldReject() {

        Message<?> message =
            subscription(
                "/user/queue/questions",
                null);

        assertThrows(
            AuthenticationCredentialsNotFoundException.class,
            () -> interceptor.preSend(
                message,
                null));
    }

    @Test
    void subscribe_unsupportedDestination_shouldReject() {

        Message<?> message =
            subscription(
                "/topic/unsupported",
                authenticatedUser());

        assertThrows(
            AccessDeniedException.class,
            () -> interceptor.preSend(
                message,
                null));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository);
    }

    @Test
    void sendFrame_shouldAlwaysReject() {

        StompHeaderAccessor accessor =
            StompHeaderAccessor.create(
                StompCommand.SEND);

        accessor.setDestination(
            "/app/questions");

        Message<byte[]> message =
            MessageBuilder.createMessage(
                new byte[0],
                accessor.getMessageHeaders());

        assertThrows(
            AccessDeniedException.class,
            () -> interceptor.preSend(
                message,
                null));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository);
    }

    private Message<byte[]> subscription(
        String destination,
        Authentication authentication) {

        StompHeaderAccessor accessor =
            StompHeaderAccessor.create(
                StompCommand.SUBSCRIBE);

        accessor.setDestination(
            destination);

        if (authentication != null) {
            accessor.setUser(
                authentication);
        }

        return MessageBuilder.createMessage(
            new byte[0],
            accessor.getMessageHeaders());
    }

    private Authentication authenticatedUser(
        String... authorities) {

        List<GrantedAuthority> grantedAuthorities =
            Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return new UsernamePasswordAuthenticationToken(
            "test-user",
            null,
            grantedAuthorities);
    }
}