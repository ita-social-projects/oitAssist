package com.itasocialacademy.oitassist.security.service.interfaces;

import com.itasocialacademy.oitassist.security.dao.dto.request.TokenRequest;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorVerifyRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.LoginResponse;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;

public interface TokenService {
    LoginResponse generateToken(TokenRequest tokenRequest);

    TokenResponse refreshToken(String refreshToken);

    /**
     * Completes a {@code TWO_FA_VERIFICATION_REQUIRED} login: validates the pending
     * token and submitted code, then issues a full access+refresh pair exactly as
     * {@link #generateToken} would have on a direct {@code SUCCESS}.
     *
     * @param request the pending token from {@link #generateToken} plus the code
     *                the user is submitting
     * @return a full token pair
     */
    TokenResponse verifyTwoFactor(TwoFactorVerifyRequest request);
}