package com.itasocialacademy.oitassist.user.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;
import java.time.Instant;
import java.util.Map;

public class ActivationTokenSendingTimeoutException extends BusinessException {
    public ActivationTokenSendingTimeoutException(Instant sendingAvailableAfter) {
        super("Activation token sending timeout. Please try again later.", ErrorCode.ACTIVATION_EMAIL_SENDING_TIMEOUT,
            Map.of("sendingAvailableAfter", sendingAvailableAfter));
    }
}
