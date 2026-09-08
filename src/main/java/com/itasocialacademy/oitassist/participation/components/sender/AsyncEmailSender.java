package com.itasocialacademy.oitassist.participation.components.sender;

import com.itasocialacademy.oitassist.core.properties.WebClientProperties;
import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import com.itasocialacademy.oitassist.participation.dao.dto.event.ApplicationDecisionListEvent;
import com.itasocialacademy.oitassist.participation.dao.dto.event.ApplicationDecisionEvent;
import com.itasocialacademy.oitassist.participation.dao.dto.event.InvitationRequestEvent;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
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

    private static final String PROFILE_PARAM = "profileLink";
    private static final String COMPETITION_LINK_PARAM = "competitionLink";
    private static final String COMPETITION_TITLE_PARAM = "competitionTitle";
    private static final String STAGE_TITLE_PARAM = "stageTitle";
    private static final String NAME_PARAM = "firstName";

    /**
     * Handles asynchronously an {@link ApplicationDecisionEvent} after the
     * publishing transaction has committed. Builds an URL (or URLs) and sends an
     * application status email.
     *
     * <p>
     * According to the request status the corresponding template is sent. In case
     * of the ACCEPTED status the email contains only the link for specific
     * {@code competition stage} and the application-accepted template is sent. In
     * case of the REJECTED one the email has links both for the {@code competition}
     * and student's {@code profile} and the application-rejected template is sent.
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
        String competitionLink = buildLink(COMPETITION_PATH);
        String profileLink = buildLink(PROFILE_PATH);
        log.info("Handling ApplicationDecisionEvent for email={}, status={}", email, status);

        String template = determineTemplate(status);
        Map<String, String> extraParams = new HashMap<>();
        if (status == RequestStatus.REJECTED) {
            if (event.rejectionReason() != null && !event.rejectionReason().isBlank()) {
                extraParams.put("rejectionReason", event.rejectionReason());
            }
            extraParams.put(PROFILE_PARAM, profileLink);
        }
        Map<String, String> root = new HashMap<>(Map.of(
            NAME_PARAM, event.firstName(),
            COMPETITION_TITLE_PARAM, event.competitionTitle(),
            STAGE_TITLE_PARAM, event.stageTitle(),
            COMPETITION_LINK_PARAM, competitionLink));
        root.putAll(extraParams);

        emailService.sendTemplateEmail(
            email,
            template,
            "Статус заявки",
            root);
    }

    /**
     * Handles asynchronously an {@link InvitationRequestEvent} after the publishing
     * transaction has committed. Builds the URLs and sends an invitation email for
     * each student.
     *
     * @param event the event carrying the titles of competition and stage, the list
     *              of recipients with emails and first names
     */
    @Async
    public void sendInvitationEmail(InvitationRequestEvent event) {
        log.info("Handling InvitationRequestEvent for {} users", event.users().size());

        String competitionLink = buildLink(COMPETITION_PATH);
        String profileLink = buildLink(PROFILE_PATH);
        for (UserProfileDetails user : event.users()) {
            Map<String, String> root = Map.of(
                NAME_PARAM, user.firstName(),
                COMPETITION_TITLE_PARAM, event.competitionTitle(),
                STAGE_TITLE_PARAM, event.stageTitle(),
                COMPETITION_LINK_PARAM, competitionLink,
                PROFILE_PARAM, profileLink);
            emailService.sendTemplateEmail(
                user.email(),
                "invitation-request.html",
                "Запрошення на олімпіаду",
                root);
        }
    }

    /**
     * Handles asynchronously an {@link ApplicationDecisionListEvent} after the
     * publishing transaction has committed. Builds the URLs and sends an
     * application status email for each student.
     *
     * <p>
     * According to the request status the corresponding template is sent. In case
     * of the ACCEPTED status the emails contain only the link for specific
     * {@code competition stage} and the application-accepted templates are sent. In
     * case of the REJECTED one the emails have links both for the
     * {@code competition stage} and student's {@code profile} and the
     * application-rejected templates are sent.
     * </p>
     *
     * @param event the event carrying the titles of competition and stage, the list
     *              of recipients with emails and first names, the rejection reason
     *              (for the REJECTED case) and the requests' status
     */

    @Async
    public void sendDecisionEmailList(ApplicationDecisionListEvent event) {
        log.info("Handling ApplicationDecisionListEvent for {} users", event.users().size());

        String competitionLink = buildLink(COMPETITION_PATH);
        String profileLink = buildLink(PROFILE_PATH);
        RequestStatus status = event.status();
        String template = determineTemplate(status);
        Map<String, String> extraParams = new HashMap<>();
        if (status == RequestStatus.REJECTED) {
            if (event.rejectionReason() != null && !event.rejectionReason().isBlank()) {
                extraParams.put("rejectionReason", event.rejectionReason());
            }
            extraParams.put(PROFILE_PARAM, profileLink);
        }
        for (UserProfileDetails user : event.users()) {
            Map<String, String> root = new HashMap<>(Map.of(
                NAME_PARAM, user.firstName(),
                COMPETITION_TITLE_PARAM, event.competitionTitle(),
                STAGE_TITLE_PARAM, event.stageTitle(),
                COMPETITION_LINK_PARAM, competitionLink));
            root.putAll(extraParams);
            emailService.sendTemplateEmail(
                user.email(),
                template,
                "Статус заявки",
                root);
        }
    }

    private String buildLink(String link) {
        return UriComponentsBuilder
            .fromUriString(webClientProperties.origin())
            .path(link)
            .build()
            .toUriString();
    }

    private String determineTemplate(RequestStatus status) {
        return switch (status) {
            case ACCEPTED -> "application-accepted.html";
            case REJECTED -> "application-rejected.html";
            default -> throw new IllegalArgumentException("No email template for status: " + status);
        };
    }
}
