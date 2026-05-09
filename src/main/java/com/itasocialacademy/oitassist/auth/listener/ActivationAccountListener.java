package com.itasocialacademy.oitassist.auth.listener;

import com.itasocialacademy.oitassist.core.properties.WebClientProperties;
import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import com.itasocialacademy.oitassist.user.api.events.UserRegisteredEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Listens for {@link UserRegisteredEvent} published by the {@code user} module
 * and sends the account activation email asynchronously after the publishing
 * transaction commits.
 *
 * <p>
 * This listener is the only {@code auth}-side consumer of
 * {@link UserRegisteredEvent}. Keeping it in {@code auth} maintains the
 * separation: {@code user} owns the domain event (published after state
 * transitions on the {@code User} aggregate), while {@code auth} owns the email
 * delivery infrastructure.
 * </p>
 *
 * <p>
 * The handler is async ({@code @Async}) so that email delivery does not block
 * the originating registration or resend transaction thread. The
 * {@code AFTER_COMMIT} phase guarantees the event is only processed when the
 * user and token are safely persisted.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivationAccountListener {
    private final EmailService emailService;
    private final WebClientProperties webClientProperties;

    private static final String CONFIRM_REGISTRATION_PATH = "/confirm_registration";

    /**
     * Handles a {@link UserRegisteredEvent} after the publishing transaction has
     * committed. Builds the activation URL from the event token and sends the
     * registration confirmation email.
     *
     * @param event the event carrying the recipient email, first name, and raw
     *              activation token
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Handling UserRegisteredEvent for email={}", event.email());

        String activationLink = UriComponentsBuilder
            .fromUriString(webClientProperties.origin())
            .path(CONFIRM_REGISTRATION_PATH)
            .queryParam("token", event.token())
            .build()
            .toUriString();

        Map<String, String> root = Map.of(
            "name", event.firstName(),
            "link", activationLink);

        emailService.sendTemplateEmail(
            event.email(),
            "registration-confirmation.html",
            "Підтвердження реєстрації",
            root);

        log.info("Activation email sent to email={}", event.email());
    }
}