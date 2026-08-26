package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO representing the each user's details of each enrollment request in the list.")
public record UserSummary(
    @Schema(description = "User's first name", example = "Ivan") String firstName,
    @Schema(description = "User's surname", example = "Petrenko") String surname,
    @Schema(description = "User's email address", example = "ivapetr@gmail.com") String email) {
}
