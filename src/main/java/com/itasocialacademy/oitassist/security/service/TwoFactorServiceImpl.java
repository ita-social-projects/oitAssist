package com.itasocialacademy.oitassist.security.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorConfirmRequest;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorEnrollRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TwoFactorEnrollResponse;
import com.itasocialacademy.oitassist.security.dao.enums.TwoFactorMethod;
import com.itasocialacademy.oitassist.security.dao.model.UserRecoveryCode;
import com.itasocialacademy.oitassist.security.dao.model.UserTwoFactorAuth;
import com.itasocialacademy.oitassist.security.dao.repository.UserRecoveryCodeRepository;
import com.itasocialacademy.oitassist.security.dao.repository.UserTwoFactorAuthRepository;
import com.itasocialacademy.oitassist.security.exceptions.InvalidTwoFactorCodeException;
import com.itasocialacademy.oitassist.security.exceptions.TwoFactorAlreadyEnabledException;
import com.itasocialacademy.oitassist.security.exceptions.TwoFactorEnrollmentNotFoundException;
import com.itasocialacademy.oitassist.security.properties.TwoFactorProperties;
import com.itasocialacademy.oitassist.security.service.interfaces.TwoFactorService;
import com.itasocialacademy.oitassist.security.twofactor.TotpProvider;
import com.itasocialacademy.oitassist.security.twofactor.TwoFactorOtpRequestedEvent;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link TwoFactorService}. Covers enrollment only, per plan
 * sequencing step 3 — {@code /2fa/verify} (login-time verification) is added
 * once the JWT wiring is connected (steps 5-7).
 *
 * <p>
 * <b>Email-OTP is now fully wired</b> (plan sequencing step 4): {@link #enroll}
 * generates and stores a hashed, expiring pending code on the entity for the
 * {@code EMAIL_OTP} path, then publishes {@link TwoFactorOtpRequestedEvent}.
 * {@code TwoFactorOtpListener} (in the {@code twofactor} package) consumes it
 * after the enrolling transaction commits and sends the code by email —
 * mirroring {@code auth}'s {@code ActivationAccountEvent}/{@code Listener}
 * pattern.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class TwoFactorServiceImpl implements TwoFactorService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Alphabet for recovery codes: uppercase alphanumeric with ambiguous characters
     * removed (0/O, 1/I/L) so a user transcribing a saved code by hand doesn't
     * stumble over lookalikes.
     */
    private static final String RECOVERY_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private final UserTwoFactorAuthRepository twoFactorAuthRepository;
    private final UserRecoveryCodeRepository recoveryCodeRepository;
    private final TotpProvider totpProvider;
    private final PasswordEncoder passwordEncoder;
    private final TwoFactorProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public TwoFactorEnrollResponse enroll(Long userId, String userEmail, TwoFactorEnrollRequest request) {
        discardAnyUnconfirmedPriorAttempt(userId);

        TwoFactorMethod method = request.getMethod();
        EnrollmentSetup setup = switch (method) {
            case TOTP -> startTotpEnrollment(userId, userEmail);
            case EMAIL_OTP -> startEmailOtpEnrollment(userId, userEmail);
        };

        UserTwoFactorAuth savedEntity = twoFactorAuthRepository.save(setup.entity());

        List<String> plaintextRecoveryCodes = generateRecoveryCodes();
        persistRecoveryCodes(savedEntity.getId(), plaintextRecoveryCodes);

        return TwoFactorEnrollResponse.builder()
            .method(method.name())
            .provisioningUri(setup.provisioningUri())
            .secret(setup.secret())
            .recoveryCodes(plaintextRecoveryCodes)
            .build();
    }

    private EnrollmentSetup startTotpEnrollment(Long userId, String userEmail) {
        String secret = totpProvider.generateSecret();
        String provisioningUri = totpProvider.buildProvisioningUri(secret, userEmail);
        UserTwoFactorAuth entity = UserTwoFactorAuth.startEnrollment(userId, TwoFactorMethod.TOTP, secret);
        return new EnrollmentSetup(entity, secret, provisioningUri);
    }

    private EnrollmentSetup startEmailOtpEnrollment(Long userId, String userEmail) {
        UserTwoFactorAuth entity = UserTwoFactorAuth.startEnrollment(userId, TwoFactorMethod.EMAIL_OTP, null);
        String plaintextOtp = issuePendingEmailOtp(entity);
        eventPublisher.publishEvent(new TwoFactorOtpRequestedEvent(userEmail, plaintextOtp));
        return new EnrollmentSetup(entity, null, null);
    }

    /**
     * Everything {@link #enroll} needs out of a method-specific setup step, bundled
     * so the two branches ({@link #startTotpEnrollment},
     * {@link #startEmailOtpEnrollment}) can each return one immutable value instead
     * of the caller pre-declaring nullable locals and conditionally assigning them.
     * {@code secret}/{@code provisioningUri} are null for {@code EMAIL_OTP} by
     * design — see {@link TwoFactorEnrollResponse}'s field docs, which document the
     * same nullability at the API boundary.
     */
    private record EnrollmentSetup(UserTwoFactorAuth entity, String secret, String provisioningUri) {
    }

    @Override
    @Transactional
    public void confirmEnrollment(Long userId, TwoFactorConfirmRequest request) {
        UserTwoFactorAuth entity = twoFactorAuthRepository.findByUserId(userId)
            .orElseThrow(() -> new TwoFactorEnrollmentNotFoundException(
                "No two-factor enrollment in progress for this user", ErrorCode.TWO_FACTOR_ENROLLMENT_NOT_FOUND));

        boolean valid = switch (entity.getMethod()) {
            case TOTP -> confirmTotp(entity, request.getCode());
            case EMAIL_OTP -> confirmEmailOtp(entity, request.getCode());
        };

        if (!valid) {
            throw new InvalidTwoFactorCodeException(
                "Invalid verification code", ErrorCode.INVALID_TWO_FACTOR_CODE);
        }

        entity.confirmEnabled();
        twoFactorAuthRepository.save(entity);
    }

    private void discardAnyUnconfirmedPriorAttempt(Long userId) {
        twoFactorAuthRepository.findByUserId(userId).ifPresent(existing -> {
            if (existing.isEnabled()) {
                throw new TwoFactorAlreadyEnabledException(
                    "Two-factor authentication is already enabled for this account; "
                        + "use /2fa/change-method to switch methods",
                    ErrorCode.TWO_FACTOR_ALREADY_ENABLED);
            }
            // Unconfirmed attempt — nothing was ever protected by it, safe to replace
            // outright.
            recoveryCodeRepository.deleteByTwoFactorAuthId(existing.getId());
            twoFactorAuthRepository.delete(existing);
        });
    }

    private boolean confirmTotp(UserTwoFactorAuth entity, String code) {
        return totpProvider.verify(entity.getTotpSecret(), code)
            .map(bucket -> {
                entity.recordTotpUse(bucket);
                return true;
            })
            .orElse(false);
    }

    private boolean confirmEmailOtp(UserTwoFactorAuth entity, String code) {
        if (entity.getPendingEmailOtpHash() == null || entity.isPendingEmailOtpExpired()) {
            return false;
        }
        boolean matches = passwordEncoder.matches(code, entity.getPendingEmailOtpHash());
        if (matches) {
            entity.clearPendingEmailOtp();
        }
        return matches;
    }

    private String issuePendingEmailOtp(UserTwoFactorAuth entity) {
        String plaintextOtp = generateNumericOtp();
        Instant expiresAt = Instant.now().plusMillis(properties.getEmailOtpValidityMillis());
        entity.setPendingEmailOtp(passwordEncoder.encode(plaintextOtp), expiresAt);
        return plaintextOtp;
    }

    private String generateNumericOtp() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private List<String> generateRecoveryCodes() {
        int count = properties.getRecoveryCodeCount();
        int length = properties.getRecoveryCodeLength();
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            codes.add(generateSingleRecoveryCode(length));
        }
        return codes;
    }

    private String generateSingleRecoveryCode(int length) {
        StringBuilder code = new StringBuilder(length + 1);
        for (int i = 0; i < length; i++) {
            if (i == length / 2) {
                code.append('-');
            }
            code.append(RECOVERY_CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(RECOVERY_CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private void persistRecoveryCodes(Long twoFactorAuthId, List<String> plaintextCodes) {
        List<UserRecoveryCode> entities = plaintextCodes.stream()
            .map(plain -> UserRecoveryCode.issue(twoFactorAuthId, passwordEncoder.encode(plain)))
            .toList();
        recoveryCodeRepository.saveAll(entities);
    }
}