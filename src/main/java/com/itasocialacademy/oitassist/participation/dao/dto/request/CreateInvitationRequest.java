package com.itasocialacademy.oitassist.participation.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Schema(description = "DTO for creating a new Invitation. "
    + "Newly created invitations will initially have the PENDING status.")
public class CreateInvitationRequest extends EnrollmentRequest {
    @Schema(
        description = "List of students IDs",
        type = "array", example = "[1, 3, 4]",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@NotNull Long> studentIds;
}
