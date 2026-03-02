package com.itasocialacademy.oitassist.core.service.interfaces;

import java.util.Map;

/**
 * Service responsible for sending emails. Provides functionality for delivering
 * HTML emails based on templates. Implementations may integrate with external
 * SMTP providers or third-party email delivery services.
 */
public interface EmailService {
    /**
     * Sends an HTML email generated from a template.
     *
     * @param to           recipient email address
     * @param templatePath path to the email template file (e.g. FreeMarker,
     *                     Thymeleaf template)
     * @param root         template model containing variables used for rendering
     *                     dynamic content
     *
     * @throws RuntimeException if email sending fails
     */
    void sendHtmlEmail(String to, String templatePath, String subject, Map<String, Object> root);
}
