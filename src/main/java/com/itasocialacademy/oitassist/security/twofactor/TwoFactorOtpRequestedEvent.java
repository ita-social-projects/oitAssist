package com.itasocialacademy.oitassist.security.twofactor;

import com.itasocialacademy.oitassist.security.service.TwoFactorServiceImpl;

/**
 * Published when an {@code EMAIL_OTP} enrollment (or, later, a login-time
 * email-OTP request) needs a code delivered by email.
 *
 * <p>
 * Deliberately minimal, mirroring
 * {@code auth.dto.event.ActivationAccountEvent}'s shape — carries only raw
 * data, not anything derived. Unlike {@code ActivationAccountEvent}, there's no
 * {@code firstName}: at the point {@link TwoFactorServiceImpl#enroll} publishes
 * this, only {@code userId} and {@code userEmail} are in scope (the
 * {@code security} module cannot reach into {@code user} for a display name),
 * so the email intentionally uses a generic greeting rather than a personalized
 * one.
 * </p>
 */
public record TwoFactorOtpRequestedEvent(
    String email,
    String code) {
}