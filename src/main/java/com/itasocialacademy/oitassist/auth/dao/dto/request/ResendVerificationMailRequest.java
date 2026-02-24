package com.itasocialacademy.oitassist.auth.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(description = "Request object for user registration")
public class ResendVerificationMailRequest {
    @Schema(
        description = "User email address",
        example = "test@mail.com",
        format = "email")
    @NotBlank
    @Email
    private String email;
}
