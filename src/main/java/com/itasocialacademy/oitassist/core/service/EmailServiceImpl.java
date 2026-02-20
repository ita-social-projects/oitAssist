package com.itasocialacademy.oitassist.core.service;

import com.itasocialacademy.oitassist.core.exceptions.EmailSendingException;
import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender javaMailSender;
    private final Configuration configuration;

    /**
     * {@inheritDoc}
     */
    @Async
    @Override
    public void sendHtmlEmail(String to, String templatePath, String subject, Map<String, Object> root) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);

            Template t = configuration.getTemplate(templatePath);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(FreeMarkerTemplateUtils.processTemplateIntoString(t, root), true);

            javaMailSender.send(message);
        } catch (MessagingException | IOException | TemplateException e) {
            throw new EmailSendingException(e);
        }
    }
}
