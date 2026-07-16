package com.itasocialacademy.oitassist.user.dao.dto.request;

import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for changing user status")
public class ChangeUserStatusRequest {
    @Schema(description = "New status to assign to the user", example = "ACTIVE")
    @NotNull
    private UserStatus status;
}