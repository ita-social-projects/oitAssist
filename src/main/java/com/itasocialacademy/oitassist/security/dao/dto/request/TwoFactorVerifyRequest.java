package com.itasocialacademy.oitassist.security.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Two-factor login verification request dto")
public class TwoFactorVerifyRequest {
    @NotBlank
    @Schema(description = "The pendingTwoFactorToken returned by /signIn when outcome=TWO_FA_VERIFICATION_REQUIRED")
    private String pendingTwoFactorToken;

    @NotBlank
    @Schema(description = "The TOTP code, email-OTP code, or a recovery code", example = "123456")
    private String code;
}