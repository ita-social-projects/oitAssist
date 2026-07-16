package com.itasocialacademy.oitassist.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.springframework.modulith.NamedInterface;

@Schema(description = "Current User DTO")
@NamedInterface("CurrentUserDTO")
public record CurrentUserDTO(
        Long id,
        @NotBlank @Schema(description = "User Email", example = "mail@gmail.com") String email,
        @NotBlank @Schema(description = "User First name", example = "Bob") String firstName,
        @NotBlank @Schema(description = "User Last name", example = "Bob") String lastName,
        @Schema(description = "User Middle name", example = "Bob") String middleName,
        @Schema(description = "User Phone Number", example = "380931111111") String phoneNumber
) {
    public Long getId() {
        return id();
    }
}