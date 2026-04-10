package com.itasocialacademy.oitassist.core.service;

import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender javaMailSender;
    private final Configuration configuration;

    /**
     * {@inheritDoc}
     *
     * <p>Loads the FreeMarker template at {@code templatePath}, merges it with the
     * provided {@code root} model, and sends the rendered HTML as a MIME message.
     * Runs asynchronously. Errors during template processing or mail delivery are
     * logged and swallowed — the caller is not notified of send failures.
     */
    @Async
    @Override
    public void sendTemplateEmail(String to, String templatePath, String subject, Map<String, String> root) {
        log.info("Sending email to={} with template={}", to, templatePath);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            Template t = configuration.getTemplate(templatePath);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(FreeMarkerTemplateUtils.processTemplateIntoString(t, root), true);

            javaMailSender.send(message);

            log.info("Email successfully sent to={}", to);
        } catch (MessagingException | IOException | TemplateException e) {
            log.error("Failed to send email to={}", to, e);
        }
    }
}
