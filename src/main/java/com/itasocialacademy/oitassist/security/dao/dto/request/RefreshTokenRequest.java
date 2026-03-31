package com.itasocialacademy.oitassist.security.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Refresh Token Request dto")
public class RefreshTokenRequest {
    @NotBlank
    @Schema(description = "Refresh Token", example = "token")
    private String token;
}
