package com.itasocialacademy.oitassist.user.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Schema(description = "Create User request DTO")
public class CreateUserDTO {
    @NotBlank
    @Schema(description = "User Email", example = "mail@gmail.com")
    private String email;
    @NotBlank
    @Schema(description = "User Password", example = "password")
    private String password;
    @NotBlank
    @Schema(description = "User First name", example = "Bob")
    private String firstName;
    @NotBlank
    @Schema(description = "User Last name", example = "Bob")
    private String lastName;
    @NotBlank
    @Schema(description = "User Middle name", example = "Bob")
    private String middleName;
    @NotBlank
    @Schema(description = "User Phone Number", example = "380931111111")
    private String phoneNumber;
}
