package com.itasocialacademy.oitassist.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for resend activation mail")
public class ResendVerificationMailRequest {
    @Schema(
        description = "User email address",
        example = "test@mail.com",
        format = "email")
    @NotBlank
    @Email
    private String email;
}
