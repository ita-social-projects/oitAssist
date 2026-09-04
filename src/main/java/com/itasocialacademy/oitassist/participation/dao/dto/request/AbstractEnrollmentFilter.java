package com.itasocialacademy.oitassist.participation.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractEnrollmentFilter {
    @Schema(
        description = "Unique identifier of the competition",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long competitionId;
    @Schema(
        description = "Unique identifier of the stage",
        example = "2",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long stageId;
}
