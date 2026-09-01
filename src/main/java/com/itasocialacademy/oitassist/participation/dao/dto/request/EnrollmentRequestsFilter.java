package com.itasocialacademy.oitassist.participation.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Schema(description = "DTO for getting the enrollment requests list.")
public class EnrollmentRequestsFilter extends AbstractEnrollmentFilter {
}
