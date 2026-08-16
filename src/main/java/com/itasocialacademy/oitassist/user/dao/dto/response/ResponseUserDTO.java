package com.itasocialacademy.oitassist.user.dao.dto.response;

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
@Schema(description = "User Response DTO")
public class ResponseUserDTO {
    @Schema(description = "User ID", example = "1")
    private Long id;
    @NotBlank
    @Schema(description = "User Email", example = "mail@gmail.com")
    private String email;
    @NotBlank
    @Schema(description = "User First name", example = "Bob")
    private String firstName;
    @NotBlank
    @Schema(description = "User Last name", example = "Bob")
    private String lastName;
    @Schema(description = "User Middle name", example = "Bob")
    private String middleName;
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
