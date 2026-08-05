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

    // TODO: the route link should be agreed with the frontend-part later
    private static final String COMPETITION_PATH = "/competitions";

    @Async
    public void sendDecisionEmail(ApplicationAcceptedEvent event) {
        String email = event.email();
        log.info("Handling ApplicationAcceptedEvent for email={}", email);

        String link = UriComponentsBuilder
            .fromUriString(webClientProperties.origin())
            .path(COMPETITION_PATH)
            .build()
            .toUriString();

        Map<String, String> root = Map.of(
            "firstName", event.firstName(),
            "competitionTitle", event.competitionTitle(),
            "stageTitle", event.stageTitle(),
            "link", link);
        emailService.sendTemplateEmail(
            email,
            "application-accepted.html",
            "Статус заявки",
            root);
        log.info("Accepted application email sent to email={}", email);
    }
}
