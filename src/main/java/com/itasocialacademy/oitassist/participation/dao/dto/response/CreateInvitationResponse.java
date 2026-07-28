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
    private List<SucceededInvitationResponse> succeeded;
    private List<FailedInvitationResponse> failed;
}
