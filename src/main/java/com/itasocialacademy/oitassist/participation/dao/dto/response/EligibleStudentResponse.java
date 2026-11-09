package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO representing a student eligible for stage invitation")
public record EligibleStudentResponse(
    @Schema(description = "User ID", example = "105")
    Long id,

    @Schema(description = "User's first name", example = "Олександр")
    String firstName,

    @Schema(description = "User's surname", example = "Коваленко")
    String surname,

    @Schema(description = "User's email", example = "olex.koval@gmail.com")
    String email) {
}
