package com.itasocialacademy.oitassist.usercompetition.dao.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.enums.UserCompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetitionId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Schema(description = "Update UserCompetition request DTO")
public class UpdateUserCompetitionDTO implements UpdateEntityDTO<UserCompetitionId> {
    private UserCompetitionId id;

    @NotNull
    @Schema(description = "Initial status of the user-competition relation", example = "INVITED")
    private UserCompetitionStatus status;

    @Schema(description = "Whether the invitation has been marked as read", example = "true")
    private boolean isRead;

}
