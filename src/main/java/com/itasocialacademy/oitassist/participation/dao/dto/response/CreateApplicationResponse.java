package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Schema(description = "DTO representing an Application creation response")
public class CreateApplicationResponse extends EnrollmentResponse {
    @Schema(description = "Unique identifier of the enrollment request", example = "1")
    private Long id;
    @Schema(description = "Current request status", example = "PENDING")
    private RequestStatus status;
}
