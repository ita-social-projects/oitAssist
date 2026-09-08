package com.itasocialacademy.oitassist.participation.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO for creating a new Invitation. "
    + "Newly created invitations will initially have the PENDING status.")
public class CreateInvitationRequest {
    @Schema(
        description = "List of students IDs",
        type = "array", example = "[1, 3, 4]",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Size(max = 200)
    private List<@NotNull Long> studentIds;
}
