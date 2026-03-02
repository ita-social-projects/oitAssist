package com.itasocialacademy.oitassist.core.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import java.io.IOException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for Email Service")
class EmailServiceImplTest {
    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private Configuration configuration;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    @DisplayName("Send HTML email with template")
    void sendHtmlEmail_templateFound_shouldSendEmail() throws Exception {
        // given
        String to = "test@mail.com";
        String templatePath = "registration-confirmation.html";
        String subject = "Registration Confirmation";
        Map<String, Object> root = Map.of("name", "Test");
        MimeMessage message = mock(MimeMessage.class);
        Template template = mock(Template.class);

        // when
        when(javaMailSender.createMimeMessage()).thenReturn(message);
        when(configuration.getTemplate(templatePath)).thenReturn(template);

        emailService.sendHtmlEmail(to, templatePath, subject, root);

        // then
        verify(javaMailSender, times(1)).send(message);
    }

    @Test
    @DisplayName("Send HTML email with template - IOException")
    void sendHtmlEmail_whenMessagingException_shouldThrowEmailSendingException() throws Exception {
        // given
        String to = "test@mail.com";
        String templatePath = "registration-confirmation.html";
        Map<String, Object> root = Map.of("name", "Test");
        String subject = "Registration Confirmation";
        MimeMessage message = mock(MimeMessage.class);

        // when
        when(javaMailSender.createMimeMessage()).thenReturn(message);
        doThrow(new IOException()).when(configuration).getTemplate(templatePath);

        // then
        assertDoesNotThrow(() -> emailService.sendHtmlEmail(to, templatePath, subject, root));
    }
}
