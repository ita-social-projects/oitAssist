package com.itasocialacademy.oitassist.security.dao.dto.response;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class TokenResponse {
    private String token;
}
