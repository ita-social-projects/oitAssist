package com.itasocialacademy.oitassist.user.dao.dto.request;

import com.itasocialacademy.oitassist.user.dao.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for changing user role")
public class ChangeUserRoleRequest {
    @Schema(description = "New role to assign to the user", example = "USER")
    @NotNull
    private Role role;
}