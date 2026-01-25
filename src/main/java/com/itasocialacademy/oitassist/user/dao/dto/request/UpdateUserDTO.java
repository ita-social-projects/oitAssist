package com.itasocialacademy.oitassist.user.dao.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateUserDTO implements UpdateEntityDTO<Long> {
    private Long id;
    @NotBlank
    private String email;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String middleName;
    @NotBlank
    private String phoneNumber;
    @NotBlank
    @NotNull
    private Role role;
    @NotBlank
    @NotNull
    private UserStatus status;
}
