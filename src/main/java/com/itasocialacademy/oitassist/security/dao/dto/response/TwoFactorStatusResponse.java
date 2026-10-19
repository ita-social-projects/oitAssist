package com.itasocialacademy.oitassist.security.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Current two-factor authentication status for the authenticated user")
public class TwoFactorStatusResponse {
    @Schema(description = "Whether 2FA is confirmed and enforced on this account", example = "true")
    private boolean enabled;

    @Schema(description = "The confirmed method, if enabled. Null otherwise — including when an "
        + "unconfirmed enrollment exists but was never confirmed.", example = "TOTP")
    private String method;
}
