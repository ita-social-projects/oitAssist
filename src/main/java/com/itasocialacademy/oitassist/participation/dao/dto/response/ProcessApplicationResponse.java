package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Schema(description = "DTO representing an Application processing response")
public class ProcessApplicationResponse extends ProcessEnrollmentResponse {
    @Schema(description = "ID of the user who last updated the application", example = "5")
    Long processedBy;
}
