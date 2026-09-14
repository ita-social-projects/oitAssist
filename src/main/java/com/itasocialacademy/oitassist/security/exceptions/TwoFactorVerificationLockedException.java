package com.itasocialacademy.oitassist.security.exceptions;

public class TwoFactorVerificationLockedException extends RuntimeException {
  public TwoFactorVerificationLockedException(String message) {
    super(message);
  }
}
