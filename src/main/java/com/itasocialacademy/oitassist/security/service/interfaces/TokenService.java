package com.itasocialacademy.oitassist.security.service.interfaces;

import com.itasocialacademy.oitassist.security.dao.dto.request.TokenRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.LoginResponse;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;

public interface TokenService {
    LoginResponse generateToken(TokenRequest tokenRequest);

    TokenResponse refreshToken(String refreshToken);
}
