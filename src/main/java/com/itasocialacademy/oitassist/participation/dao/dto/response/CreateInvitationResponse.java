package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Schema(description = "DTO representing an Invitation creation response")
public class CreateInvitationResponse extends EnrollmentResponse {
    @Schema(
        description = "List of students IDs",
        type = "array", example = "[1, 3, 4]",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> studentIds;
}
