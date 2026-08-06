package com.itasocialacademy.oitassist.participation.sender;

import com.itasocialacademy.oitassist.core.properties.WebClientProperties;
import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import com.itasocialacademy.oitassist.participation.dao.dto.event.ApplicationDecisionEvent;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncEmailSender {
    private final EmailService emailService;
    private final WebClientProperties webClientProperties;

    // TODO: the route links should be agreed with the frontend-part later
    private static final String COMPETITION_PATH = "/competitions";
    private static final String PROFILE_PATH = "/profile";

    /**
     * Handles asynchronously an {@link ApplicationDecisionEvent} after the
     * publishing transaction has committed. Builds the activation URL from the
     * event token and sends the email.
     *
     * <p>
     * Based on the request status the corresponding template is sent. In case of
     * ACCEPTED event the email contains only the link for {@code competition}. In
     * case of REJECTED one the email has links for {@code competition} and
     * {@code profile}.
     * </p>
     *
     * @param event the event carrying the titles of competition and stage, the
     *              recipient's email and first name, the request status and the
     *              rejection reason
     */
    @Async
    public void sendDecisionEmail(ApplicationDecisionEvent event) {
        String email = event.email();
        RequestStatus status = event.status();
        log.info("Handling ApplicationDecisionEvent for email={}, status={}", email, status);

        String template = switch (status) {
            case ACCEPTED -> "application-accepted.html";
            case REJECTED -> "application-rejected.html";
            default -> throw new IllegalArgumentException("No email template for status: " + status);
        };
        Map<String, String> extraParams = new HashMap<>();
        if (status == RequestStatus.REJECTED) {
            if (event.rejectionReason() != null && !event.rejectionReason().isBlank()) {
                extraParams.put("rejectionReason", event.rejectionReason());
            }
            String profileLink = UriComponentsBuilder
                .fromUriString(webClientProperties.origin())
                .path(PROFILE_PATH)
                .build()
                .toUriString();
            extraParams.put("profileLink", profileLink);
        }

        String competitionLink = UriComponentsBuilder
            .fromUriString(webClientProperties.origin())
            .path(COMPETITION_PATH)
            .build()
            .toUriString();
        Map<String, String> root = new HashMap<>(Map.of(
            "firstName", event.firstName(),
            "competitionTitle", event.competitionTitle(),
            "stageTitle", event.stageTitle(),
            "competitionLink", competitionLink));
        root.putAll(extraParams);

        emailService.sendTemplateEmail(
            email,
            template,
            "Статус заявки",
            root);
    }
}
