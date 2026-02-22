package com.itasocialacademy.oitassist.user.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Request object for user registration")
public class CreateUserRequest {
    @Schema(
        description = "User email address",
        example = "test@mail.com",
        format = "email")
    @NotBlank
    @Email
    private String email;

    @Schema(
        description = "User password",
        example = "StrongPassword123!")
    @NotBlank
    @Size(min = 8, max = 50)
    private String password;

    @Schema(
        description = "User first name",
        example = "Name",
        minLength = 2,
        maxLength = 50)
    @NotBlank
    @Size(min = 2, max = 50)
    private String firstName;

    @Schema(
        description = "User last name",
        example = "LastName",
        minLength = 2,
        maxLength = 50)
    @NotBlank
    @Size(min = 2, max = 50)
    private String lastName;

    @Schema(
        description = "User middle name",
        example = "MiddleName",
        minLength = 2,
        maxLength = 50)
    @NotBlank
    @Size(min = 2, max = 50)
    private String middleName;

    @Schema(
        description = "User phone number",
        example = "+380123456789")
    @NotBlank
    private String phoneNumber;
}
