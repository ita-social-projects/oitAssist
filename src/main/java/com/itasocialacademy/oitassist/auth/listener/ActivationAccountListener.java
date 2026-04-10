package com.itasocialacademy.oitassist.auth.listener;

import com.itasocialacademy.oitassist.auth.dto.event.ActivationAccountEvent;
import com.itasocialacademy.oitassist.core.properties.WebClientProperties;
import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Spring event listener responsible for sending account activation emails.
 * Listens for
 * {@link com.itasocialacademy.oitassist.auth.dto.event.ActivationAccountEvent}
 * events published after a successful transaction commit and dispatches a
 * verification email containing the activation link. The handler runs
 * asynchronously to avoid blocking the originating transaction thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivationAccountListener {
    private final EmailService emailService;

    private final WebClientProperties webClientProperties;

    private static final String CONFIRM_REGISTRATION_PATH = "/confirm_registration";

    /**
     * Handles an {@link ActivationAccountEvent} after the publishing transaction
     * has committed. Builds a time-limited activation URL from the event token,
     * then renders and sends the registration confirmation email to the user.
     *
     * @param event the activation event carrying the recipient email, first name,
     *              and raw activation token
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(ActivationAccountEvent event) {
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