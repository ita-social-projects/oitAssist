package com.itasocialacademy.oitassist.security.dao.dto.request;

import com.itasocialacademy.oitassist.security.dao.enums.TwoFactorMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Two-factor enrollment request dto")
public class TwoFactorEnrollRequest {
    @NotNull
    @Schema(description = "Method to enroll", example = "TOTP")
    private TwoFactorMethod method;

    @Schema(description = "The pendingTwoFactorToken from /signIn (outcome=TWO_FA_SETUP_REQUIRED). "
        + "Required only when there is no normal authenticated session — a mandatory-role user completing "
        + "forced first-time setup has nothing else to identify themselves with. Omit when calling this as an "
        + "already-logged-in user voluntarily opting in to 2FA; identity is then taken from the normal "
        + "Bearer session instead.")
    private String pendingTwoFactorToken;
}