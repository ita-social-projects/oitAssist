package com.itasocialacademy.oitassist.user.dao.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Schema(description = "Profile change request DTO")
public class ProfileUpdateRequestDTO implements CreateEntityDTO<Long> {
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be between 1 and 50 characters")
    @Schema(description = "User First name", example = "Bob")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be between 1 and 50 characters")
    @Schema(description = "User Last name", example = "Bob")
    private String lastName;

    @Size(max = 50, message = "Middle name must be at most 50 characters")
    @Schema(description = "User Middle name", example = "Bob")
    private String middleName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    @Schema(description = "User Phone Number", example = "380931111111")
    private String phoneNumber;
}
