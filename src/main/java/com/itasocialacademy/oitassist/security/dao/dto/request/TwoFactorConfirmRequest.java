package com.itasocialacademy.oitassist.security.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Two-factor enrollment confirmation request dto")
public class TwoFactorConfirmRequest {
    @NotBlank
    @Schema(description = "The code produced by the just-configured method, proving enrollment succeeded",
        example = "123456")
    private String code;

    @Schema(description = "The pendingTwoFactorToken from /signIn (outcome=TWO_FA_SETUP_REQUIRED). Same "
        + "rule as TwoFactorEnrollRequest — required only when there is no normal authenticated session.")
    private String pendingTwoFactorToken;
}