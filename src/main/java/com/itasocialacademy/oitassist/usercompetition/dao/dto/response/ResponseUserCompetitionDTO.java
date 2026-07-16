package com.itasocialacademy.oitassist.usercompetition.dao.dto.response;

import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.enums.UserCompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetitionId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "UserCompetition response DTO")
public class ResponseUserCompetitionDTO implements EntityDTO<UserCompetitionId> {

    @Schema(description = "ID of the user", example = "1")
    private Long authorId;

    @Schema(description = "ID of the competition", example = "1")
    private Long competitionId;

    @Schema(description = "Current status of the user-competition relation", example = "INVITED")
    private UserCompetitionStatus status;

    @Schema(description = "Whether the invitation has been read by the user", example = "false")
    private boolean isRead;
}