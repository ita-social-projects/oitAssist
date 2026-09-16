package com.itasocialacademy.oitassist.security.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.*;

/**
 * Returned once, immediately after regeneration — same "shown once, never
 * retrievable again" rule as {@link TwoFactorEnrollResponse}'s recovery codes.
 * Wrapped in its own type rather than returning a bare {@code List<String>},
 * matching how every other endpoint in this codebase returns a proper response
 * DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Two-factor recovery codes response dto")
public class TwoFactorRecoveryCodesResponse {
    @Schema(description = "Ten new plaintext recovery codes, replacing every previously unused one. "
        + "Shown once — never retrievable again after this response.")
    private List<String> recoveryCodes;
}