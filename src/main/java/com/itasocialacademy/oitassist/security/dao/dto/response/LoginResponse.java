package com.itasocialacademy.oitassist.security.dao.dto.response;

import com.itasocialacademy.oitassist.security.dao.enums.LoginOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login response dto, discriminated by outcome")
public class LoginResponse {
    @Schema(description = "Which outcome this login attempt resulted in", example = "SUCCESS")
    private LoginOutcome outcome;

    @Schema(description = "Full access+refresh token pair. Present only when outcome=SUCCESS; "
        + "null otherwise.")
    private TokenResponse tokens;

    @Schema(description = "Short-lived token to submit to /2fa/verify or /2fa/enroll. Present for "
        + "both non-SUCCESS outcomes; null for SUCCESS.")
    private String pendingTwoFactorToken;

    @Schema(description = "Which 2FA method to prompt the user for. Present only when "
        + "outcome=TWO_FA_VERIFICATION_REQUIRED (there's nothing to name for TWO_FA_SETUP_REQUIRED, "
        + "since no method is configured yet); null otherwise.", example = "TOTP")
    private String twoFactorMethod;
}