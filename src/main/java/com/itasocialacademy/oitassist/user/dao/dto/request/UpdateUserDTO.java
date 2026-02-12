package com.itasocialacademy.oitassist.user.dao.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(description = "Update User request DTO")
public class UpdateUserDTO implements UpdateEntityDTO<Long> {
    private Long id;
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
    @NotBlank
    @NotNull
    @Schema(description = "User Role", example = "USER")
    private Role role;
    @NotBlank
    @NotNull
    @Schema(description = "User Status", example = "ACTIVATED")
    private UserStatus status;
}
