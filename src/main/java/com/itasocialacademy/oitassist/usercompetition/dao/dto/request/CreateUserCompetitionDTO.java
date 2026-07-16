package com.itasocialacademy.oitassist.usercompetition.dao.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.enums.UserCompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetitionId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Schema(description = "Create UserCompetition request DTO")
public class CreateUserCompetitionDTO implements CreateEntityDTO<UserCompetitionId> {
    @NotNull
    @Schema(description = "ID of the user being invited to the competition", example = "1")
    private Long authorId;

    @NotNull
    @Schema(description = "ID of the competition that user being invited to", example = "1")
    private Long competitionId;

    @NotNull
    @Schema(description = "Initial status of the user-competition relation", example = "INVITED")
    private UserCompetitionStatus status;

}
