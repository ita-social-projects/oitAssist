package com.itasocialacademy.oitassist.participation.sender;

import com.itasocialacademy.oitassist.core.properties.WebClientProperties;
import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import com.itasocialacademy.oitassist.participation.dao.dto.event.ApplicationAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncEmailSender {
    private final EmailService emailService;
    private final WebClientProperties webClientProperties;

    @Async
    public void sendDecisionEmail(ApplicationAcceptedEvent event) {
        String email = event.email();

        String link = UriComponentsBuilder
            .fromUriString(webClientProperties.origin())
            .path("/users/profile")
            .build()
            .toUriString();

        Map<String, String> root = Map.of(
            "competitionTitle", event.competitionTitle(),
            "stageTitle", event.stageTitle(),
            "link", link
        );
        try {
            emailService.sendTemplateEmail(email, "application-accepted.html", "Прийнята заявка", root);
        } catch (Exception e) {
            log.error("Failed to send decision email to {}", email, e);
        }
    }
}
