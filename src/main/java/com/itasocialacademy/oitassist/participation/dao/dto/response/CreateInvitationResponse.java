package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Schema(description = "DTO representing an Invitation creation response")
public class CreateInvitationResponse extends EnrollmentResponse {
    @Schema(
        description = "List of students IDs that were successfully invited",
        type = "array", example = "[1, 3, 4]")
    private List<Long> succeeded;
    @Schema(
        description = "List of students IDs that were failed to be invited",
        type = "array", example = "[2, 5]")
    private List<Long> failed;
}
