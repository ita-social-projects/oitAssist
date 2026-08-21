package com.itasocialacademy.oitassist.security.twofactor;

import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import com.itasocialacademy.oitassist.security.properties.TwoFactorProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link TwoFactorOtpRequestedEvent} and sends the email-OTP code
 * asynchronously after the publishing transaction commits.
 *
 * <p>
 * Structurally mirrors {@code auth.listener.ActivationAccountListener}: same
 * {@code @Async} + {@code @TransactionalEventListener(phase = AFTER_COMMIT)}
 * combination, so delivery doesn't block the enrollment thread and only ever
 * fires once the hashed code is safely persisted — never for a code that might
 * not actually exist yet if the surrounding transaction rolled back.
 * </p>
 *
 * <p>
 * Where {@code ActivationAccountListener} injects {@code WebClientProperties}
 * to build a clickable link, this injects {@link TwoFactorProperties} instead,
 * to compute how many minutes the code stays valid for the email copy — the
 * same "combine event data with the listener's own config to build the template
 * model" pattern, just with different config for a different kind of email.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwoFactorOtpListener {
    private static final String TEMPLATE_PATH = "two-factor-otp.html";
    private static final String SUBJECT = "Код підтвердження двофакторної автентифікації";
    private static final long MILLIS_PER_MINUTE = 60_000L;

    private final EmailService emailService;
    private final TwoFactorProperties properties;

    /**
     * Handles a {@link TwoFactorOtpRequestedEvent} after the publishing transaction
     * has committed. Builds the email model (code + how long it's valid for) and
     * sends the email-OTP message.
     *
     * @param event the event carrying the recipient email and plaintext code
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTwoFactorOtpRequested(TwoFactorOtpRequestedEvent event) {
        log.info("Handling TwoFactorOtpRequestedEvent for email={}", event.email());

        long validityMinutes = properties.getEmailOtpValidityMillis() / MILLIS_PER_MINUTE;

        Map<String, String> root = Map.of(
            "code", event.code(),
            "validityMinutes", String.valueOf(validityMinutes));

        emailService.sendTemplateEmail(event.email(), TEMPLATE_PATH, SUBJECT, root);

        log.info("Two-factor OTP email sent to email={}", event.email());
    }
}