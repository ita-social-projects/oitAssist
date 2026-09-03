package com.itasocialacademy.oitassist.security.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorConfirmRequest;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorEnrollRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TwoFactorEnrollResponse;
import com.itasocialacademy.oitassist.security.dao.dto.response.TwoFactorStatusResponse;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link TwoFactorService}: enrollment, confirmation, and
 * login-time verification.
 *
 * <p>
 * Email-OTP dispatch (plan sequencing step 4) publishes
 * {@link TwoFactorOtpRequestedEvent} after the enrolling/verifying transaction
 * commits; {@code TwoFactorOtpListener} (in the {@code twofactor} package)
 * consumes it and sends the code by email — mirroring {@code auth}'s
 * {@code ActivationAccountEvent}/{@code Listener} pattern.
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

        UserTwoFactorAuth savedEntity;
        try {
            savedEntity = twoFactorAuthRepository.save(setup.entity());
        } catch (DataIntegrityViolationException e) {
            throw new TwoFactorAlreadyEnabledException(
                "An enrollment attempt is already in progress for this account; please retry",
                ErrorCode.TWO_FACTOR_ALREADY_ENABLED);
        }

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
        issueAndDispatchEmailOtp(entity, userEmail);
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
        UserTwoFactorAuth entity = twoFactorAuthRepository.findByUserIdForUpdate(userId)
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

    @Override
    @Transactional
    public void verify(Long userId, String code) {
        UserTwoFactorAuth entity = findEnabledTwoFactorAuth(userId);

        boolean valid = switch (entity.getMethod()) {
            case TOTP -> confirmTotp(entity, code);
            case EMAIL_OTP -> confirmEmailOtp(entity, code);
        };
        if (!valid) {
            valid = tryRecoveryCode(entity, code);
        }

        if (!valid) {
            throw new InvalidTwoFactorCodeException(
                "Invalid verification code", ErrorCode.INVALID_TWO_FACTOR_CODE);
        }

        twoFactorAuthRepository.save(entity);
    }

    @Override
    @Transactional
    public void resendLoginOtp(Long userId, String userEmail) {
        UserTwoFactorAuth entity = findEnabledTwoFactorAuth(userId);
        issueAndDispatchEmailOtp(entity, userEmail);
        twoFactorAuthRepository.save(entity);
    }

    @Override
    @Transactional
    public List<String> regenerateRecoveryCodes(Long userId) {
        UserTwoFactorAuth entity = findEnabledTwoFactorAuth(userId);

        recoveryCodeRepository.deleteByTwoFactorAuthId(entity.getId());

        List<String> plaintextRecoveryCodes = generateRecoveryCodes();
        persistRecoveryCodes(entity.getId(), plaintextRecoveryCodes);

        return plaintextRecoveryCodes;
    }

    @Override
    public TwoFactorStatusResponse getStatus(Long userId) {
        return twoFactorAuthRepository.findByUserId(userId)
            .filter(UserTwoFactorAuth::isEnabled)
            .map(entity -> TwoFactorStatusResponse.builder()
                .enabled(true)
                .method(entity.getMethod().name())
                .build())
            .orElse(TwoFactorStatusResponse.builder().enabled(false).build());
    }

    private UserTwoFactorAuth findEnabledTwoFactorAuth(Long userId) {
        return twoFactorAuthRepository.findByUserIdForUpdate(userId)
            .filter(UserTwoFactorAuth::isEnabled)
            .orElseThrow(() -> new TwoFactorEnrollmentNotFoundException(
                "No active two-factor setup found for this user", ErrorCode.TWO_FACTOR_ENROLLMENT_NOT_FOUND));
    }

    /**
     * Tries a candidate string against every unused recovery code's hash. There's
     * no way to look a hash up by plaintext directly (that's the point of hashing)
     * — this is the same linear-scan-and-compare approach password verification
     * always uses, just over a handful of hashes (at most
     * {@code recoveryCodeCount}, typically 10) instead of one.
     */
    private boolean tryRecoveryCode(UserTwoFactorAuth entity, String code) {
        List<UserRecoveryCode> unusedCodes =
            recoveryCodeRepository.findByTwoFactorAuthIdAndUsedFalse(entity.getId());

        for (UserRecoveryCode candidate : unusedCodes) {
            if (passwordEncoder.matches(code, candidate.getCodeHash())) {
                candidate.markUsed();
                recoveryCodeRepository.save(candidate);
                return true;
            }
        }
        return false;
    }

    private void discardAnyUnconfirmedPriorAttempt(Long userId) {
        twoFactorAuthRepository.findByUserId(userId).ifPresent(existing -> {
            if (existing.isEnabled()) {
                throw new TwoFactorAlreadyEnabledException(
                    "Two-factor authentication is already enabled for this account; "
                        + "use /2fa/change-method to switch methods",
                    ErrorCode.TWO_FACTOR_ALREADY_ENABLED);
            }
            recoveryCodeRepository.deleteByTwoFactorAuthId(existing.getId());
            twoFactorAuthRepository.delete(existing);
            twoFactorAuthRepository.flush();
        });
    }

    private boolean confirmTotp(UserTwoFactorAuth entity, String code) {
        return totpProvider.verify(entity.getTotpSecret(), code)
            .filter(bucket -> !entity.isTotpBucketAlreadyUsed(bucket))
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

    /**
     * Shared by {@link #startEmailOtpEnrollment} and {@link #resendLoginOtp}:
     * generate a fresh code, hash+store it on the entity, and publish the event
     * that dispatches it by email. Kept as one method rather than duplicated at
     * both call sites — enrollment and login-time resend need the exact same
     * "generate, store, dispatch" sequence, just applied to a freshly-built entity
     * vs. one already fetched from the repository.
     */
    private void issueAndDispatchEmailOtp(UserTwoFactorAuth entity, String userEmail) {
        String plaintextOtp = issuePendingEmailOtp(entity);
        eventPublisher.publishEvent(new TwoFactorOtpRequestedEvent(userEmail, plaintextOtp));
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