package com.itasocialacademy.oitassist.core.service.interfaces;

import java.util.Map;

/**
 * Service responsible for sending emails. Provides functionality for delivering
 * HTML emails based on templates. Implementations may integrate with external
 * SMTP providers or third-party email delivery services.
 */
public interface EmailService {
    /**
     * Sends an HTML email using a specified template. The method takes a
     * recipient's email address, a path to the email template, a subject line, and
     * a map of key-value pairs for populating the template. The method processes
     * the email template with the provided data and sends the resulting email.
     *
     * @param to           the recipient's email address
     * @param templatePath the file path of the email template to be used
     * @param subject      the subject line of the email
     * @param root         a map containing key-value pairs used to populate
     *                     placeholders in the email template
     */
    void sendTemplateEmail(String to, String templatePath, String subject, Map<String, String> root);
}
