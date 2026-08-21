package com.itasocialacademy.oitassist.security.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.*;

/**
 * Returned once, immediately after {@code enroll()}. Recovery codes are
 * plaintext here and only here — see plan section 2.3: they are hashed before
 * persistence and are never retrievable again after this response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Two-factor enrollment response dto")
public class TwoFactorEnrollResponse {
    @Schema(description = "The method being enrolled", example = "TOTP")
    private String method;

    @Schema(description = "otpauth:// provisioning URI to render as a QR code. Null for EMAIL_OTP.",
        example = "otpauth://totp/OITAssist:john@example.com?secret=...&issuer=OITAssist")
    private String provisioningUri;

    @Schema(description = "The raw base32 secret, for manual entry if the user can't scan a QR code. "
        + "Null for EMAIL_OTP.", example = "JBSWY3DPEHPK3PXP")
    private String secret;

    @Schema(description = "Ten plaintext one-time recovery codes. Shown exactly once — "
        + "never retrievable again after this response.")
    private List<String> recoveryCodes;
}