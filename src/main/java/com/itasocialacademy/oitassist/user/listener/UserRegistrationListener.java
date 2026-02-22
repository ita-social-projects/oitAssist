package com.itasocialacademy.oitassist.user.listener;

import com.itasocialacademy.oitassist.core.properties.WebClientProperties;
import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import com.itasocialacademy.oitassist.user.dao.dto.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationListener {
    private final EmailService emailService;

    private final WebClientProperties webClientProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Handling UserRegisteredEvent for email={}", event.email());

        String activationLink =
            webClientProperties.origin() + "/confirm_registration?token=" + event.token();

        Map<String, Object> root = new HashMap<>();
        root.put("name", event.firstName());
        root.put("link", activationLink);

        emailService.sendHtmlEmail(
            event.email(),
            "registration-confirmation.html",
            "Підтвердження реєстрації",
            root);

        log.info("Activation email sent to email={}", event.email());
    }
}