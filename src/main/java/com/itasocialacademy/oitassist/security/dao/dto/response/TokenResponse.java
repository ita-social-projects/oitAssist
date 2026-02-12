package com.itasocialacademy.oitassist.security.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Schema(description = "Token Response to return token")
public class TokenResponse {
    @Schema(description = "token", examples = "very-long-token")
    private String token;
}
