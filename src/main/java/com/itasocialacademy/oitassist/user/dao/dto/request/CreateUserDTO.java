package com.itasocialacademy.oitassist.user.dao.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserDTO implements CreateEntityDTO<Long> {
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String middleName;
    @NotBlank
    private String phoneNumber;
}
